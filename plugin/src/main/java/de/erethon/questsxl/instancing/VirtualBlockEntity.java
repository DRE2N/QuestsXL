package de.erethon.questsxl.instancing;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.bukkit.inventory.Inventory;

/**
 * Represents a virtualized block entity (container) state for instanced areas.
 * This handles chests, furnaces, barrels, and other container-type blocks.
 */
public class VirtualBlockEntity {

    private final BlockPos position;
    private final BlockEntityType<?> type;
    private CompoundTag data;

    /**
     * Runtime Bukkit inventory for player interaction.
     * Created lazily when a player opens the container.
     */
    private transient Inventory virtualInventory;

    /**
     * Tracks whether the block entity has been modified since last save.
     */
    private boolean dirty = false;

    public VirtualBlockEntity(BlockPos position, BlockEntityType<?> type) {
        this.position = position;
        this.type = type;
        this.data = new CompoundTag();
    }

    public VirtualBlockEntity(BlockPos position, BlockEntityType<?> type, CompoundTag data) {
        this.position = position;
        this.type = type;
        this.data = data != null ? data.copy() : new CompoundTag();
    }

    /**
     * Gets or creates the virtual inventory for this block entity.
     * The inventory is backed by the NBT data.
     */
    public Inventory getOrCreateInventory() {
        if (virtualInventory == null) {
            virtualInventory = InstanceInventoryFactory.createInventory(this);
        }
        return virtualInventory;
    }

    /**
     * Syncs the current inventory state back to NBT data.
     * Should be called when the inventory is closed or periodically.
     */
    public void syncInventoryToData() {
        if (virtualInventory != null) {
            de.erethon.questsxl.QuestsXL.log("[VirtualBlockEntity] Syncing inventory to NBT at " + position +
                        ", items in inventory: " + countItems(virtualInventory));
            InstanceInventoryFactory.syncToNbt(virtualInventory, this);
            de.erethon.questsxl.QuestsXL.log("[VirtualBlockEntity] After sync, NBT keys: " + data.keySet());
            dirty = true;
        }
    }

    private int countItems(org.bukkit.inventory.Inventory inv) {
        int count = 0;
        for (org.bukkit.inventory.ItemStack item : inv.getContents()) {
            if (item != null && !item.getType().isAir()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Clears the cached inventory, forcing recreation on next access.
     */
    public void clearCachedInventory() {
        virtualInventory = null;
    }

    // Getters and setters

    public BlockPos getPosition() {
        return position;
    }

    public BlockEntityType<?> getType() {
        return type;
    }

    public CompoundTag getData() {
        return data;
    }

    public void setData(CompoundTag data) {
        this.data = data;
        this.dirty = true;
        clearCachedInventory(); // Force inventory recreation with new data
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    /**
     * Creates a copy of this virtual block entity.
     */
    public VirtualBlockEntity copy() {
        return new VirtualBlockEntity(position, type, data.copy());
    }
}

