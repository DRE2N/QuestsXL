package de.erethon.questsxl.instancing;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.util.BoundingBox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for capturing and serializing block regions.
 * Handles the conversion between world state and instance template data.
 */
public class InstanceBlockCapture {

    /**
     * Captures a region of blocks from the world into the provided maps.
     * Stores block states in a simple position-to-state map for the region.
     *
     * @param world The world to capture from
     * @param bounds The bounding box defining the region
     * @param baseBlocks Output map for block states (position -> state)
     * @param baseBlockEntities Output map for block entity NBT data
     */
    public static void captureRegion(
            World world,
            BoundingBox bounds,
            Map<BlockPos, BlockState> baseBlocks,
            Map<BlockPos, CompoundTag> baseBlockEntities
    ) {
        ServerLevel level = ((CraftWorld) world).getHandle();

        int minX = (int) Math.floor(bounds.getMinX());
        int minY = (int) Math.floor(bounds.getMinY());
        int minZ = (int) Math.floor(bounds.getMinZ());
        int maxX = (int) Math.floor(bounds.getMaxX());
        int maxY = (int) Math.floor(bounds.getMaxY());
        int maxZ = (int) Math.floor(bounds.getMaxZ());

        // Capture all blocks in the region
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    // Store non-air blocks
                    if (!state.isAir()) {
                        baseBlocks.put(pos.immutable(), state);
                    }
                }
            }
        }

        // Calculate chunk bounds for block entity capture
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        // Capture block entities
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);

                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    if (isWithinBounds(pos, bounds)) {
                        BlockEntity blockEntity = entry.getValue();
                        // Use saveWithFullMetadata to include the block entity type ID in the NBT
                        CompoundTag nbt = blockEntity.saveWithFullMetadata(level.registryAccess());
                        baseBlockEntities.put(pos.immutable(), nbt);
                    }
                }
            }
        }
    }

    /**
     * Checks if a block position is within the given bounds.
     */
    private static boolean isWithinBounds(BlockPos pos, BoundingBox bounds) {
        return bounds.contains(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Serializes block data to a compressed byte array for database storage.
     * Uses Codec-based serialization for block states.
     */
    public static byte[] serializeBlockData(Map<BlockPos, BlockState> blocks) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            RegistryOps<Tag> registryOps = MinecraftServer.getServer().registryAccess()
                    .createSerializationContext(NbtOps.INSTANCE);

            dos.writeInt(blocks.size());

            for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();

                // Write position
                dos.writeInt(pos.getX());
                dos.writeInt(pos.getY());
                dos.writeInt(pos.getZ());

                // Serialize block state using Codec
                CompoundTag stateTag = (CompoundTag) BlockState.CODEC
                        .encodeStart(registryOps, state)
                        .resultOrPartial(error -> {})
                        .orElseGet(CompoundTag::new);

                NbtIo.write(stateTag, dos);
            }

            gzos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize block data", e);
        }
    }

    /**
     * Deserializes block data from a compressed byte array.
     */
    public static Map<BlockPos, BlockState> deserializeBlockData(byte[] data) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();

        if (data == null || data.length == 0) {
            return blocks;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             DataInputStream dis = new DataInputStream(gzis)) {

            RegistryOps<Tag> registryOps = MinecraftServer.getServer().registryAccess()
                    .createSerializationContext(NbtOps.INSTANCE);

            int count = dis.readInt();

            for (int i = 0; i < count; i++) {
                int x = dis.readInt();
                int y = dis.readInt();
                int z = dis.readInt();
                BlockPos pos = new BlockPos(x, y, z);

                CompoundTag stateTag = NbtIo.read(dis);

                // Deserialize block state using Codec
                BlockState state = BlockState.CODEC
                        .parse(registryOps, stateTag)
                        .resultOrPartial(error -> {})
                        .orElse(Blocks.AIR.defaultBlockState());

                blocks.put(pos, state);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize block data", e);
        }

        return blocks;
    }

    /**
     * Serializes block entity data to a compressed byte array.
     */
    public static byte[] serializeBlockEntities(Map<BlockPos, CompoundTag> blockEntities) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            dos.writeInt(blockEntities.size());

            for (Map.Entry<BlockPos, CompoundTag> entry : blockEntities.entrySet()) {
                BlockPos pos = entry.getKey();
                CompoundTag nbt = entry.getValue();

                dos.writeInt(pos.getX());
                dos.writeInt(pos.getY());
                dos.writeInt(pos.getZ());
                NbtIo.write(nbt, dos);
            }

            gzos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize block entities", e);
        }
    }

    /**
     * Deserializes block entity data from a compressed byte array.
     */
    public static Map<BlockPos, CompoundTag> deserializeBlockEntities(byte[] data) {
        Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();

        if (data == null || data.length == 0) {
            return blockEntities;
        }

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
                blockEntities.put(pos, nbt);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize block entities", e);
        }

        return blockEntities;
    }

    /**
     * Gets the block state at a position directly from the world.
     */
    public static BlockState getBlockStateFromWorld(World world, BlockPos pos) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        return level.getBlockState(pos);
    }
}

