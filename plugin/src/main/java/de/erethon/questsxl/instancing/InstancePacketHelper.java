package de.erethon.questsxl.instancing;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * Helper class for sending instance-related packets to players.
 */
public class InstancePacketHelper {

    /**
     * Sends a block update packet to a player.
     *
     * @param player The player to send to
     * @param pos The block position
     * @param state The block state to show
     */
    public static void sendBlockUpdate(Player player, BlockPos pos, BlockState state) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        ClientboundBlockUpdatePacket packet = new ClientboundBlockUpdatePacket(pos, state);
        serverPlayer.connection.send(packet);
    }

    /**
     * Sends a block update packet using Bukkit Location.
     */
    public static void sendBlockUpdate(Player player, org.bukkit.Location location, BlockState state) {
        BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        sendBlockUpdate(player, pos, state);
    }

    /**
     * Refreshes a chunk for a player by sending forget + resend.
     * This forces the client to request the chunk again, allowing packet interception.
     *
     * @param player The player
     * @param world The world
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     */
    public static void refreshChunk(Player player, World world, int chunkX, int chunkZ) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        ServerLevel level = ((CraftWorld) world).getHandle();

        // Send forget packet
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        ClientboundForgetLevelChunkPacket forgetPacket = new ClientboundForgetLevelChunkPacket(chunkPos);
        serverPlayer.connection.send(forgetPacket);

        // Re-send the chunk data
        // Schedule this to happen after a tick to ensure the forget is processed
        Bukkit.getScheduler().runTaskLater(
                de.erethon.questsxl.QuestsXL.get(),
                () -> sendChunk(player, world, chunkX, chunkZ),
                1L
        );
    }

    /**
     * Sends a full chunk packet to a player.
     */
    public static void sendChunk(Player player, World world, int chunkX, int chunkZ) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        ServerLevel level = ((CraftWorld) world).getHandle();

        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        LevelLightEngine lightEngine = level.getLightEngine();

        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                chunk,
                lightEngine,
                null, // sky light bits
                null  // block light bits
        );

        serverPlayer.connection.send(packet);
    }

    /**
     * Sends multiple block updates efficiently.
     *
     * @param player The player
     * @param updates Map of positions to block states
     */
    public static void sendMultiBlockUpdate(Player player, java.util.Map<BlockPos, BlockState> updates) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        // Group updates by chunk section for efficient sending
        java.util.Map<SectionPos, java.util.List<java.util.Map.Entry<BlockPos, BlockState>>> bySection =
                new java.util.HashMap<>();

        for (var entry : updates.entrySet()) {
            SectionPos sectionPos = SectionPos.of(entry.getKey());
            bySection.computeIfAbsent(sectionPos, k -> new java.util.ArrayList<>()).add(entry);
        }

        // Send updates for each section
        for (var sectionEntry : bySection.entrySet()) {
            SectionPos sectionPos = sectionEntry.getKey();
            var sectionUpdates = sectionEntry.getValue();

            if (sectionUpdates.size() == 1) {
                // Single block update
                var update = sectionUpdates.get(0);
                sendBlockUpdate(player, update.getKey(), update.getValue());
            } else {
                // Multiple block update
                sendSectionBlocksUpdate(serverPlayer, sectionPos, sectionUpdates);
            }
        }
    }

    /**
     * Sends a section blocks update packet for multiple blocks in the same section.
     */
    private static void sendSectionBlocksUpdate(
            ServerPlayer player,
            SectionPos sectionPos,
            java.util.List<java.util.Map.Entry<BlockPos, BlockState>> updates
    ) {
        // Create and send ClientboundSectionBlocksUpdatePacket
        short[] positions = new short[updates.size()];
        BlockState[] states = new BlockState[updates.size()];

        for (int i = 0; i < updates.size(); i++) {
            var entry = updates.get(i);
            BlockPos pos = entry.getKey();

            // Calculate local position within section
            int localX = pos.getX() & 15;
            int localY = pos.getY() & 15;
            int localZ = pos.getZ() & 15;
            positions[i] = (short) (localX << 8 | localZ << 4 | localY);
            states[i] = entry.getValue();
        }

        net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket packet =
                new net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket(
                        sectionPos, ShortSet.of(positions), states
                );

        player.connection.send(packet);
    }

    /**
     * Checks if a player has a chunk loaded.
     */
    public static boolean hasChunkLoaded(Player player, int chunkX, int chunkZ) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        return serverPlayer.getChunkTrackingView().contains(chunkPos);
    }

    /**
     * Gets the Bukkit block data string for a block state (for debugging).
     */
    public static String blockStateToString(BlockState state) {
        return state.toString();
    }
}

