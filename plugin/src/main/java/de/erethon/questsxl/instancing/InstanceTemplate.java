package de.erethon.questsxl.instancing;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines a region (world, pos1, pos2) with a "base" block snapshot.
 * Templates are the blueprints from which instances are created.
 */
public class InstanceTemplate {

    private final String id;
    private World world;
    private BoundingBox bounds;

    /**
     * Base block data stored by position.
     * Only non-air blocks are stored for efficiency.
     */
    private final Map<BlockPos, BlockState> baseBlocks = new HashMap<>();

    /**
     * Block entities (chests, furnaces, etc.) stored by their position.
     * The CompoundTag contains the full NBT data for the block entity.
     */
    private final Map<BlockPos, CompoundTag> baseBlockEntities = new HashMap<>();

    /**
     * Whether this template is rentable via apartment signs/proximity.
     * Templates for quest regions should not be rentable.
     */
    private boolean rentable = false;

    public InstanceTemplate(String id) {
        this.id = id;
    }

    public InstanceTemplate(String id, World world, BoundingBox bounds) {
        this.id = id;
        this.world = world;
        this.bounds = bounds;
    }

    /**
     * Captures the current world state within the bounds as the template's base state.
     * This should be called after the world region is set up as desired.
     */
    public void captureFromWorld() {
        if (world == null || bounds == null) {
            throw new IllegalStateException("World and bounds must be set before capturing");
        }

        baseBlocks.clear();
        baseBlockEntities.clear();

        InstanceBlockCapture.captureRegion(world, bounds, baseBlocks, baseBlockEntities);
    }

    /**
     * Gets the block state at a specific position from the template.
     * Returns the cached block state if available, otherwise reads from world.
     * @param pos The block position
     * @return The block state, or null if not within template bounds
     */
    public BlockState getBlockState(BlockPos pos) {
        if (!containsPosition(pos)) {
            return null;
        }

        // First check if we have it cached
        BlockState cached = baseBlocks.get(pos);
        if (cached != null) {
            return cached;
        }

        // For blocks not in our map (air blocks), read from world or return air
        return InstanceBlockCapture.getBlockStateFromWorld(world, pos);
    }

    /**
     * Checks if a position is within this template's bounds.
     */
    public boolean containsPosition(BlockPos pos) {
        return bounds != null && bounds.contains(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Gets all block positions that have cached states in this template.
     */
    public Iterable<BlockPos> getBlockPositions() {
        return baseBlocks.keySet();
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public void setBounds(BoundingBox bounds) {
        this.bounds = bounds;
    }

    public Map<BlockPos, BlockState> getBaseBlocks() {
        return baseBlocks;
    }

    public Map<BlockPos, CompoundTag> getBaseBlockEntities() {
        return baseBlockEntities;
    }

    public boolean isRentable() {
        return rentable;
    }

    public void setRentable(boolean rentable) {
        this.rentable = rentable;
    }

    /**
     * Gets the minimum block position of the template bounds.
     */
    public BlockPos getMinPos() {
        return new BlockPos(
            (int) bounds.getMinX(),
            (int) bounds.getMinY(),
            (int) bounds.getMinZ()
        );
    }

    /**
     * Gets the maximum block position of the template bounds.
     */
    public BlockPos getMaxPos() {
        return new BlockPos(
            (int) bounds.getMaxX(),
            (int) bounds.getMaxY(),
            (int) bounds.getMaxZ()
        );
    }
}
