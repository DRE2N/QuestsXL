package de.erethon.questsxl.instancing;

import de.erethon.questsxl.player.QPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime instance for a group of players, tracks modifications from the template.
 * Each InstancedArea represents an active instance that players can interact with.
 */
public class InstancedArea {

    private final String id;
    private final InstanceTemplate template;

    /**
     * Players currently participating in this instance.
     */
    private final Set<QPlayer> participants = ConcurrentHashMap.newKeySet();

    /**
     * Block modifications from the template (delta storage).
     * Only stores blocks that differ from the template.
     */
    private final Map<BlockPos, BlockState> modifiedBlocks = new ConcurrentHashMap<>();

    /**
     * Virtual block entities (containers with modified state).
     * Stores the runtime state of chests, furnaces, etc.
     */
    private final Map<BlockPos, VirtualBlockEntity> blockEntities = new ConcurrentHashMap<>();

    /**
     * The character ID that owns this instance (for persistence).
     * Can be null for temporary/shared instances.
     */
    private UUID ownerCharacterId;

    /**
     * Timestamp of when this instance was created.
     */
    private final long createdAt;

    /**
     * Timestamp of last modification.
     */
    private long lastModified;

    /**
     * Whether this instance has unsaved changes.
     */
    private boolean dirty = false;

    public InstancedArea(String id, InstanceTemplate template) {
        this.id = id;
        this.template = template;
        this.createdAt = System.currentTimeMillis();
        this.lastModified = this.createdAt;
    }

    public InstancedArea(String id, InstanceTemplate template, UUID ownerCharacterId) {
        this(id, template);
        this.ownerCharacterId = ownerCharacterId;
    }

    /**
     * Gets the effective block state at a position.
     * Returns the modified block if one exists, otherwise returns the template block.
     *
     * @param pos The block position
     * @return The effective block state
     */
    public BlockState getEffectiveBlockState(BlockPos pos) {
        // Check for modifications first
        BlockState modified = modifiedBlocks.get(pos);
        if (modified != null) {
            return modified;
        }

        // Fall back to template
        return template.getBlockState(pos);
    }

    /**
     * Sets a block in this instance, recording it as a modification from template.
     *
     * @param pos The block position
     * @param state The new block state
     */
    public void setBlock(BlockPos pos, BlockState state) {
        setBlock(pos, state, true);
    }

    /**
     * Sets a block in this instance, with option to mark as dirty.
     *
     * @param pos The block position
     * @param state The new block state
     * @param markDirty Whether to mark the instance as modified
     */
    public void setBlock(BlockPos pos, BlockState state, boolean markDirty) {
        if (!template.containsPosition(pos)) {
            return; // Position not within instance bounds
        }

        BlockState templateState = template.getBlockState(pos);
        if (templateState != null && templateState.equals(state)) {
            // Block matches template, remove from modifications
            modifiedBlocks.remove(pos);
        } else {
            modifiedBlocks.put(pos, state);
        }

        if (markDirty) {
            markModified();
        }
    }

    /**
     * Removes a block modification, reverting to template state.
     */
    public void revertBlock(BlockPos pos) {
        modifiedBlocks.remove(pos);
        markModified();
    }

    /**
     * Resets the entire instance to the template state.
     */
    public void reset() {
        modifiedBlocks.clear();
        blockEntities.clear();
        markModified();
    }

    /**
     * Checks if a position is within this instance's bounds.
     */
    public boolean containsPosition(BlockPos pos) {
        return template.containsPosition(pos);
    }

    /**
     * Checks if a position has been modified from the template.
     */
    public boolean isModified(BlockPos pos) {
        return modifiedBlocks.containsKey(pos);
    }

    /**
     * Adds a player to this instance.
     */
    public void addParticipant(QPlayer player) {
        participants.add(player);
    }

    /**
     * Removes a player from this instance.
     */
    public void removeParticipant(QPlayer player) {
        participants.remove(player);
    }

    /**
     * Checks if a player is participating in this instance.
     */
    public boolean hasParticipant(QPlayer player) {
        return participants.contains(player);
    }

    /**
     * Gets or creates a virtual block entity at a position.
     */
    public VirtualBlockEntity getOrCreateBlockEntity(BlockPos pos) {
        return blockEntities.computeIfAbsent(pos, p -> {
            // Check template for existing block entity data
            CompoundTag templateData = template.getBaseBlockEntities().get(p);
            de.erethon.questsxl.QuestsXL.log("[InstancedArea] getOrCreateBlockEntity: templateData=" + (templateData != null ? "exists" : "null"));

            if (templateData != null) {
                return VirtualBlockEntityFactory.fromNbt(p, templateData);
            }

            // No template data - check if the block at this position is a container
            // and create an empty virtual block entity for it
            BlockState state = getEffectiveBlockState(p);
            de.erethon.questsxl.QuestsXL.log("[InstancedArea] getOrCreateBlockEntity: effectiveBlockState=" + (state != null ? state : "null"));

            if (state != null) {
                return VirtualBlockEntityFactory.createForBlockState(p, state);
            }

            return null;
        });
    }

    /**
     * Gets a virtual block entity if it exists.
     */
    public VirtualBlockEntity getBlockEntity(BlockPos pos) {
        return blockEntities.get(pos);
    }

    /**
     * Sets a virtual block entity.
     */
    public void setBlockEntity(BlockPos pos, VirtualBlockEntity entity) {
        blockEntities.put(pos, entity);
        markModified();
    }

    private void markModified() {
        this.lastModified = System.currentTimeMillis();
        this.dirty = true;
    }

    // Getters

    public String getId() {
        return id;
    }

    public InstanceTemplate getTemplate() {
        return template;
    }

    public Set<QPlayer> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }

    public Map<BlockPos, BlockState> getModifiedBlocks() {
        return Collections.unmodifiableMap(modifiedBlocks);
    }

    public Map<BlockPos, VirtualBlockEntity> getBlockEntities() {
        return Collections.unmodifiableMap(blockEntities);
    }

    /**
     * Syncs all virtual block entity inventories to their NBT data.
     * Should be called before saving the instance.
     */
    public void syncAllBlockEntities() {
        for (VirtualBlockEntity entity : blockEntities.values()) {
            entity.syncInventoryToData();
        }
    }

    public UUID getOwnerCharacterId() {
        return ownerCharacterId;
    }

    public void setOwnerCharacterId(UUID ownerCharacterId) {
        this.ownerCharacterId = ownerCharacterId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * Checks if this instance has any participants.
     */
    public boolean isEmpty() {
        return participants.isEmpty();
    }
}

