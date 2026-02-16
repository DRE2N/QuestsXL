package de.erethon.questsxl.instancing;

import de.erethon.questsxl.QuestsXL;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.util.BoundingBox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Helper class for persisting instance data to the database.
 */
public class InstancePersistence {

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    /**
     * Saves a template to the database asynchronously.
     */
    public static CompletableFuture<Void> saveTemplate(InstanceDao dao, InstanceTemplate template) {
        return CompletableFuture.runAsync(() -> {
            try {
                World world = template.getWorld();
                BoundingBox bounds = template.getBounds();

                byte[] blockData = InstanceBlockCapture.serializeBlockData(template.getBaseBlocks());
                byte[] blockEntities = InstanceBlockCapture.serializeBlockEntities(
                        template.getBaseBlockEntities());

                dao.upsertTemplate(
                        template.getId(),
                        world.getName(),
                        (int) bounds.getMinX(),
                        (int) bounds.getMinY(),
                        (int) bounds.getMinZ(),
                        (int) bounds.getMaxX(),
                        (int) bounds.getMaxY(),
                        (int) bounds.getMaxZ(),
                        blockData,
                        blockEntities
                );

                // Store chunk coverage for apartment auto-load
                dao.deleteTemplateChunks(template.getId());
                int minChunkX = (int) Math.floor(bounds.getMinX()) >> 4;
                int maxChunkX = (int) Math.floor(bounds.getMaxX()) >> 4;
                int minChunkZ = (int) Math.floor(bounds.getMinZ()) >> 4;
                int maxChunkZ = (int) Math.floor(bounds.getMaxZ()) >> 4;
                for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                    for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                        dao.insertTemplateChunk(template.getId(), world.getName(), cx, cz);
                    }
                }

                QuestsXL.log("Saved template '" + template.getId() + "' to database");
            } catch (Exception e) {
                QuestsXL.log("Failed to save template: " + e.getMessage());
                e.printStackTrace();
            }
        }, executor);
    }

    /**
     * Loads a template from the database asynchronously.
     */
    public static CompletableFuture<InstanceTemplate> loadTemplate(InstanceDao dao, String templateId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                InstanceDao.TemplateData data = dao.getTemplateData(templateId);
                if (data == null) {
                    return null;
                }

                World world = Bukkit.getWorld(data.worldName);
                if (world == null) {
                    QuestsXL.log("Cannot load template '" + templateId + "': world '" +
                            data.worldName + "' not found");
                    return null;
                }

                BoundingBox bounds = new BoundingBox(
                        data.minX, data.minY, data.minZ,
                        data.maxX, data.maxY, data.maxZ
                );

                InstanceTemplate template = new InstanceTemplate(templateId, world, bounds);

                if (data.blockData != null && data.blockData.length > 0) {
                    Map<BlockPos, BlockState> blocks = InstanceBlockCapture.deserializeBlockData(data.blockData);
                    template.getBaseBlocks().putAll(blocks);
                }
                if (data.blockEntities != null && data.blockEntities.length > 0) {
                    Map<BlockPos, CompoundTag> entities = InstanceBlockCapture.deserializeBlockEntities(data.blockEntities);
                    template.getBaseBlockEntities().putAll(entities);
                }

                QuestsXL.log("Loaded template '" + templateId + "' from database");
                return template;
            } catch (Exception e) {
                QuestsXL.log("Failed to load template: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, executor);
    }

    /**
     * Saves an instance's state to the database asynchronously.
     */
    public static CompletableFuture<Void> saveInstanceState(InstanceDao dao, InstancedArea instance) {
        // Sync all block entity inventories to NBT before saving
        instance.syncAllBlockEntities();

        // Debug: Log what we're about to save
        QuestsXL.log("[InstancePersistence] Saving instance - modified blocks: " + instance.getModifiedBlocks().size() +
                    ", block entities: " + instance.getBlockEntities().size());

        return CompletableFuture.runAsync(() -> {
            try {
                UUID characterId = instance.getOwnerCharacterId();
                String templateId = instance.getTemplate().getId();

                if (characterId == null) {
                    return; // Cannot save without owner
                }

                byte[] modifiedBlocks = serializeModifiedBlocks(instance.getModifiedBlocks());
                byte[] blockEntities = serializeBlockEntities(instance.getBlockEntities());

                QuestsXL.log("[InstancePersistence] Serialized data - blocks: " + modifiedBlocks.length +
                            " bytes, entities: " + blockEntities.length + " bytes");

                dao.upsertInstanceState(characterId, templateId, modifiedBlocks, blockEntities);

                QuestsXL.log("Saved instance state for character " + characterId +
                        " template " + templateId);
            } catch (Exception e) {
                QuestsXL.log("Failed to save instance state: " + e.getMessage());
                e.printStackTrace();
            }
        }, executor);
    }

    /**
     * Loads an instance's state from the database asynchronously.
     */
    public static CompletableFuture<InstancedArea> loadInstanceState(
            InstanceDao dao,
            UUID characterId,
            String templateId,
            InstanceTemplate template
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                InstanceDao.InstanceStateData data = dao.getInstanceState(characterId, templateId);
                if (data == null) {
                    QuestsXL.log("[InstancePersistence] No saved state found for character " + characterId + " template " + templateId);
                    return null;
                }

                QuestsXL.log("[InstancePersistence] Loading saved state - blocks data: " +
                            (data.modifiedBlocks != null ? data.modifiedBlocks.length : 0) + " bytes, " +
                            "entities data: " + (data.blockEntities != null ? data.blockEntities.length : 0) + " bytes");

                String instanceId = templateId + "_" + characterId.toString().substring(0, 8);
                InstancedArea instance = new InstancedArea(instanceId, template, characterId);

                // Deserialize modified blocks
                if (data.modifiedBlocks != null && data.modifiedBlocks.length > 0) {
                    Map<BlockPos, BlockState> modified = deserializeModifiedBlocks(
                            data.modifiedBlocks,
                            ((CraftWorld) template.getWorld()).getHandle()
                    );
                    QuestsXL.log("[InstancePersistence] Loaded " + modified.size() + " modified blocks");
                    for (var entry : modified.entrySet()) {
                        instance.setBlock(entry.getKey(), entry.getValue(), false); // Don't mark dirty when loading
                    }
                }

                // Deserialize block entities
                if (data.blockEntities != null && data.blockEntities.length > 0) {
                    Map<BlockPos, VirtualBlockEntity> entities = deserializeBlockEntities(data.blockEntities);
                    QuestsXL.log("[InstancePersistence] Loaded " + entities.size() + " block entities");
                    for (var entry : entities.entrySet()) {
                        instance.setBlockEntity(entry.getKey(), entry.getValue());
                    }
                }

                instance.setDirty(false); // Just loaded, not dirty

                QuestsXL.log("Loaded instance state for character " + characterId);
                return instance;
            } catch (Exception e) {
                QuestsXL.log("Failed to load instance state: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, executor);
    }

    // ==================== Serialization Methods ====================

    private static byte[] serializeModifiedBlocks(Map<BlockPos, BlockState> blocks) {
        // Reuse the block capture serialization which uses Codecs
        return InstanceBlockCapture.serializeBlockData(blocks);
    }

    private static Map<BlockPos, BlockState> deserializeModifiedBlocks(byte[] data, ServerLevel level) {
        // Reuse the block capture deserialization which uses Codecs
        return InstanceBlockCapture.deserializeBlockData(data);
    }

    private static byte[] serializeBlockEntities(Map<BlockPos, VirtualBlockEntity> entities) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            dos.writeInt(entities.size());
            for (var entry : entities.entrySet()) {
                BlockPos pos = entry.getKey();
                VirtualBlockEntity entity = entry.getValue();

                dos.writeInt(pos.getX());
                dos.writeInt(pos.getY());
                dos.writeInt(pos.getZ());

                CompoundTag nbt = entity.getData();
                NbtIo.write(nbt, dos);
            }

            gzos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize block entities", e);
        }
    }

    private static Map<BlockPos, VirtualBlockEntity> deserializeBlockEntities(byte[] data) {
        Map<BlockPos, VirtualBlockEntity> entities = new HashMap<>();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             DataInputStream dis = new DataInputStream(gzis)) {

            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                int x = dis.readInt();
                int y = dis.readInt();
                int z = dis.readInt();
                BlockPos pos = new BlockPos(x, y, z);

                CompoundTag nbt = NbtIo.read(dis);
                VirtualBlockEntity entity = VirtualBlockEntityFactory.fromNbt(pos, nbt);
                if (entity != null) {
                    entities.put(pos, entity);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize block entities", e);
        }

        return entities;
    }

    /**
     * Shuts down the executor service.
     */
    public static void shutdown() {
        executor.shutdown();
    }
}

