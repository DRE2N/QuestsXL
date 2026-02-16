package de.erethon.questsxl.instancing;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;


/**
 * Packet handler for the instancing system.
 * Intercepts chunk and block packets to show instance-specific block data.
 * Also intercepts serverbound packets to prevent real-world modifications.
 */
public class InstancePacketListener extends ChannelDuplexHandler {

    private final QuestsXL plugin;
    private final ServerPlayer player;
    private InstanceManager instanceManager;

    /**
     * Tracks block breaking progress for survival mode.
     * Key: BlockPos, Value: start time in millis
     */
    private BlockPos currentlyBreaking = null;
    private long breakStartTime = 0;

    public InstancePacketListener(QuestsXL plugin, ServerPlayer player) {
        this.plugin = plugin;
        this.player = player;
    }

    private InstanceManager getInstanceManager() {
        if (instanceManager == null) {
            instanceManager = plugin.getInstanceManager();
        }
        return instanceManager;
    }

    // ==================== Server -> Client ====================

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        InstanceManager manager = getInstanceManager();
        if (manager == null) {
            super.write(ctx, msg, promise);
            return;
        }

        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player.getBukkitEntity());
        if (qPlayer == null) {
            super.write(ctx, msg, promise);
            return;
        }

        InstancedArea instance = manager.getActiveInstance(qPlayer);
        if (instance == null) {
            super.write(ctx, msg, promise);
            return;
        }

        // Handle different packet types
        if (msg instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
            handleChunkPacket(chunkPacket, instance, qPlayer);
        } else if (msg instanceof ClientboundBlockUpdatePacket blockPacket) {
            msg = handleBlockUpdate(blockPacket, instance);
        } else if (msg instanceof ClientboundBlockEntityDataPacket blockEntityPacket) {
            msg = handleBlockEntityPacket(blockEntityPacket, instance);
        }

        super.write(ctx, msg, promise);
    }

    // ==================== Client -> Server (Incoming) ====================

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        InstanceManager manager = getInstanceManager();
        if (manager == null) {
            super.channelRead(ctx, msg);
            return;
        }

        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player.getBukkitEntity());
        if (qPlayer == null) {
            super.channelRead(ctx, msg);
            return;
        }

        InstancedArea instance = manager.getActiveInstance(qPlayer);
        if (instance == null) {
            super.channelRead(ctx, msg);
            return;
        }

        // Handle incoming packets for block interactions
        if (msg instanceof ServerboundPlayerActionPacket actionPacket) {
            if (handlePlayerAction(ctx, actionPacket, instance, qPlayer)) {
                return; // Packet consumed, don't pass to server
            }
        } else if (msg instanceof ServerboundUseItemOnPacket useItemPacket) {
            if (handleUseItemOn(ctx, useItemPacket, instance, qPlayer)) {
                return; // Packet consumed
            }
        }

        super.channelRead(ctx, msg);
    }

    // ==================== Outgoing Packet Handlers ====================

    private void handleChunkPacket(ClientboundLevelChunkWithLightPacket packet, InstancedArea instance, QPlayer qPlayer) {
        ChunkPos chunkPos = new ChunkPos(packet.getX(), packet.getZ());
        BoundingBox bounds = instance.getTemplate().getBounds();

        int chunkMinX = chunkPos.getMinBlockX();
        int chunkMaxX = chunkPos.getMaxBlockX();
        int chunkMinZ = chunkPos.getMinBlockZ();
        int chunkMaxZ = chunkPos.getMaxBlockZ();

        if (chunkMaxX < bounds.getMinX() || chunkMinX > bounds.getMaxX() ||
            chunkMaxZ < bounds.getMinZ() || chunkMinZ > bounds.getMaxZ()) {
            return; // Chunk doesn't overlap
        }

        // Schedule block updates for this chunk to show instance data
        scheduleBlockUpdates(chunkPos, instance, qPlayer);
    }

    private Object handleBlockUpdate(ClientboundBlockUpdatePacket packet, InstancedArea instance) {
        BlockPos pos = packet.getPos();

        if (!instance.containsPosition(pos)) {
            return packet;
        }

        // Get the instance's version of this block
        BlockState instanceState = instance.getEffectiveBlockState(pos);
        if (instanceState != null && !instanceState.equals(packet.getBlockState())) {
            return new ClientboundBlockUpdatePacket(pos, instanceState);
        }

        return packet;
    }

    private Object handleBlockEntityPacket(ClientboundBlockEntityDataPacket packet, InstancedArea instance) {
        BlockPos pos = packet.getPos();

        if (!instance.containsPosition(pos)) {
            return packet;
        }

        VirtualBlockEntity virtualEntity = instance.getBlockEntity(pos);
        if (virtualEntity != null) {
            return new ClientboundBlockEntityDataPacket(pos, packet.getType(), virtualEntity.getData());
        }

        return packet;
    }

    // ==================== Incoming Packet Handlers ====================

    /**
     * Handles player action packets (block breaking, etc.)
     * Returns true if the packet was consumed and should NOT be passed to server.
     */
    private boolean handlePlayerAction(ChannelHandlerContext ctx, ServerboundPlayerActionPacket packet,
                                        InstancedArea instance, QPlayer qPlayer) {
        BlockPos pos = packet.getPos();

        if (!instance.containsPosition(pos)) {
            return false; // Not in instance bounds, let server handle
        }

        ServerboundPlayerActionPacket.Action action = packet.getAction();

        switch (action) {
            case START_DESTROY_BLOCK -> {
                if (player.getBukkitEntity().getGameMode() == GameMode.CREATIVE) {
                    // Creative mode - instant break
                    handleInstanceBlockBreak(pos, instance);
                    sendBlockBreakAck(ctx, pos, Blocks.AIR.defaultBlockState());
                } else {
                    // Survival mode - start tracking the break
                    currentlyBreaking = pos;
                    breakStartTime = System.currentTimeMillis();
                    // Send break animation start (stage 0)
                    sendBlockBreakAnimation(pos, 0);
                }
                return true;
            }
            case STOP_DESTROY_BLOCK -> {
                // Player finished breaking (or thinks they did)
                if (currentlyBreaking != null && currentlyBreaking.equals(pos)) {
                    // Complete the break
                    handleInstanceBlockBreak(pos, instance);
                    sendBlockBreakAck(ctx, pos, Blocks.AIR.defaultBlockState());
                    currentlyBreaking = null;
                    breakStartTime = 0;
                }
                return true;
            }
            case ABORT_DESTROY_BLOCK -> {
                // Player cancelled breaking
                if (currentlyBreaking != null && currentlyBreaking.equals(pos)) {
                    // Send animation cancel (stage -1 or 10)
                    sendBlockBreakAnimation(pos, -1);
                    currentlyBreaking = null;
                    breakStartTime = 0;
                }
                return true;
            }
            default -> {
                return false; // Other actions pass through
            }
        }
    }

    /**
     * Handles use item on block packets (placing blocks, opening containers)
     * Returns true if the packet was consumed.
     */
    private boolean handleUseItemOn(ChannelHandlerContext ctx, ServerboundUseItemOnPacket packet,
                                    InstancedArea instance, QPlayer qPlayer) {
        BlockPos clickedPos = packet.getHitResult().getBlockPos();

        // Check if clicked position is in instance
        boolean clickedInInstance = instance.containsPosition(clickedPos);
        BlockPos placePos = clickedPos.relative(packet.getHitResult().getDirection());
        boolean placeInInstance = instance.containsPosition(placePos);

        if (!clickedInInstance && !placeInInstance) {
            return false; // Neither position in instance
        }

        // Check if the target block is a container (only if clicking in instance)
        if (clickedInInstance) {
            BlockState state = instance.getEffectiveBlockState(clickedPos);
            if (state != null && isContainer(state)) {
                QuestsXL.log("[Instance] Container click detected at " + clickedPos + ", state: " + state);
                // Schedule container open on main thread
                Bukkit.getScheduler().runTask(plugin, () -> handleContainerOpen(clickedPos, instance, qPlayer));
                return true; // Consume packet
            }
        }

        // Handle block placement
        if (placeInInstance) {
            // Schedule block placement on main thread
            Bukkit.getScheduler().runTask(plugin, () -> handleInstanceBlockPlace(placePos, instance, qPlayer));
            return true; // Consume packet
        }

        return false;
    }

    // ==================== Instance Block Operations ====================

    private void handleInstanceBlockBreak(BlockPos pos, InstancedArea instance) {
        BlockState air = Blocks.AIR.defaultBlockState();
        instance.setBlock(pos, air);

        // Send block update to all participants
        for (QPlayer participant : instance.getParticipants()) {
            InstancePacketHelper.sendBlockUpdate(participant.getPlayer(), pos, air);
        }

        // TODO: Handle drops, tool durability, etc.
    }

    private void handleInstanceBlockPlace(BlockPos pos, InstancedArea instance, QPlayer qPlayer) {
        // Get the item the player is holding
        ItemStack itemInHand = qPlayer.getPlayer().getInventory().getItemInMainHand();
        if (itemInHand.getType().isAir()) {
            itemInHand = qPlayer.getPlayer().getInventory().getItemInOffHand();
        }

        if (itemInHand.getType().isAir() || !itemInHand.getType().isBlock()) {
            return; // Not holding a placeable block
        }

        // Convert Bukkit block to NMS block state
        org.bukkit.block.data.BlockData blockData = itemInHand.getType().createBlockData();
        BlockState nmsState = ((org.bukkit.craftbukkit.block.data.CraftBlockData) blockData).getState();

        instance.setBlock(pos, nmsState);

        // Send block update to all participants
        for (QPlayer participant : instance.getParticipants()) {
            InstancePacketHelper.sendBlockUpdate(participant.getPlayer(), pos, nmsState);
        }

        // Consume item in survival mode
        if (qPlayer.getPlayer().getGameMode() != GameMode.CREATIVE) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        }

        // Save the instance immediately to persist the change
        saveInstanceAsync(instance);
    }

    private void handleContainerOpen(BlockPos pos, InstancedArea instance, QPlayer qPlayer) {
        QuestsXL.log("[Instance] handleContainerOpen called for pos " + pos);

        VirtualBlockEntity virtualEntity = instance.getOrCreateBlockEntity(pos);
        QuestsXL.log("[Instance] VirtualBlockEntity: " + (virtualEntity != null ? virtualEntity.getType() : "null"));

        if (virtualEntity == null) {
            QuestsXL.log("[Instance] Failed to create virtual block entity!");
            return;
        }

        Inventory inventory = virtualEntity.getOrCreateInventory();
        QuestsXL.log("[Instance] Inventory: " + (inventory != null ? inventory.getType() + " size=" + inventory.getSize() : "null"));

        if (inventory != null) {
            qPlayer.getPlayer().openInventory(inventory);
            QuestsXL.log("[Instance] Opened inventory for player " + qPlayer.getPlayer().getName());
        }
    }

    // ==================== Utility Methods ====================

    private void sendBlockBreakAck(ChannelHandlerContext ctx, BlockPos pos, BlockState newState) {
        // Send block update to confirm the break to the client
        try {
            ClientboundBlockUpdatePacket updatePacket = new ClientboundBlockUpdatePacket(pos, newState);
            ctx.writeAndFlush(updatePacket);
        } catch (Exception e) {
            // Fallback - ignore errors
        }
    }

    /**
     * Sends a block break animation packet to the player.
     * @param pos The block position
     * @param stage The break stage (0-9 for progress, -1 or 10 to cancel)
     */
    private void sendBlockBreakAnimation(BlockPos pos, int stage) {
        try {
            // Use a unique entity ID for the break animation
            int entityId = pos.hashCode();
            ClientboundBlockDestructionPacket packet = new ClientboundBlockDestructionPacket(entityId, pos, stage);
            player.connection.send(packet);
        } catch (Exception e) {
            // Ignore errors
        }
    }

    private void scheduleBlockUpdates(ChunkPos chunkPos, InstancedArea instance, QPlayer qPlayer) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            InstanceManager manager = getInstanceManager();
            if (manager == null) return;

            if (manager.getActiveInstance(qPlayer) != instance) {
                return;
            }

            int chunkX = chunkPos.x;
            int chunkZ = chunkPos.z;

            int sentCount = 0;
            for (var entry : instance.getModifiedBlocks().entrySet()) {
                BlockPos pos = entry.getKey();
                if ((pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ) {
                    BlockState state = entry.getValue();
                    InstancePacketHelper.sendBlockUpdate(qPlayer.getPlayer(), pos, state);
                    sentCount++;
                }
            }

            if (sentCount > 0) {
                QuestsXL.log("[InstancePacketListener] Sent " + sentCount + " block updates for chunk (" + chunkX + ", " + chunkZ + ")");
            }
        }, 2L);
    }

    private boolean isContainer(BlockState state) {
        net.minecraft.world.level.block.Block block = state.getBlock();
        return block instanceof net.minecraft.world.level.block.ChestBlock ||
               block instanceof net.minecraft.world.level.block.BarrelBlock ||
               block instanceof net.minecraft.world.level.block.ShulkerBoxBlock ||
               block instanceof net.minecraft.world.level.block.AbstractFurnaceBlock ||
               block instanceof net.minecraft.world.level.block.HopperBlock ||
               block instanceof net.minecraft.world.level.block.DispenserBlock ||
               block instanceof net.minecraft.world.level.block.DropperBlock;
    }

    /**
     * Saves an instance asynchronously after a block change.
     */
    private void saveInstanceAsync(InstancedArea instance) {
        InstanceManager manager = getInstanceManager();
        if (manager != null && instance.isDirty()) {
            manager.saveInstance(instance).exceptionally(e -> {
                QuestsXL.log("[InstancePacketListener] Failed to save instance after block change: " + e.getMessage());
                e.printStackTrace();
                return null;
            });
        }
    }
}
