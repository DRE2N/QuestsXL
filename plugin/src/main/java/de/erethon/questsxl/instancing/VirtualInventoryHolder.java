package de.erethon.questsxl.instancing;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * InventoryHolder implementation for virtual inventories.
 */
public class VirtualInventoryHolder implements InventoryHolder {

    private final VirtualBlockEntity entity;
    private Inventory inventory;

    public VirtualInventoryHolder(VirtualBlockEntity entity) {
        this.entity = entity;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public VirtualBlockEntity getEntity() {
        return entity;
    }
}
