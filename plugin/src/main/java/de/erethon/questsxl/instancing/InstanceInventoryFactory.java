package de.erethon.questsxl.instancing;

import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Factory for creating and managing virtual inventories backed by NBT data.
 */
public class InstanceInventoryFactory {

    /**
     * Creates a Bukkit Inventory from a VirtualBlockEntity.
     */
    public static Inventory createInventory(VirtualBlockEntity entity) {
        BlockEntityType<?> type = entity.getType();
        int size = getInventorySize(type);
        String title = getInventoryTitle(type);

        // Create a virtual inventory holder
        VirtualInventoryHolder holder = new VirtualInventoryHolder(entity);
        Inventory inventory = Bukkit.createInventory(holder, size, Component.text(title));

        // Load items from NBT
        loadItemsFromNbt(inventory, entity.getData());

        return inventory;
    }

    /**
     * Syncs the current inventory contents back to the VirtualBlockEntity's NBT.
     */
    public static void syncToNbt(Inventory inventory, VirtualBlockEntity entity) {
        CompoundTag nbt = entity.getData();
        saveItemsToNbt(inventory, nbt);
    }

    /**
     * Gets the inventory size for a block entity type.
     */
    private static int getInventorySize(BlockEntityType<?> type) {
        if (type == BlockEntityType.CHEST || type == BlockEntityType.TRAPPED_CHEST) {
            return 27;
        } else if (type == BlockEntityType.BARREL) {
            return 27;
        } else if (type == BlockEntityType.SHULKER_BOX) {
            return 27;
        } else if (type == BlockEntityType.FURNACE || type == BlockEntityType.BLAST_FURNACE || type == BlockEntityType.SMOKER) {
            return 3;
        } else if (type == BlockEntityType.HOPPER) {
            return 5;
        } else if (type == BlockEntityType.DISPENSER || type == BlockEntityType.DROPPER) {
            return 9;
        } else if (type == BlockEntityType.BREWING_STAND) {
            return 5;
        }
        return 27; // Default
    }

    /**
     * Gets a display title for the inventory.
     */
    private static String getInventoryTitle(BlockEntityType<?> type) {
        if (type == BlockEntityType.CHEST || type == BlockEntityType.TRAPPED_CHEST) {
            return "Chest";
        } else if (type == BlockEntityType.BARREL) {
            return "Barrel";
        } else if (type == BlockEntityType.SHULKER_BOX) {
            return "Shulker Box";
        } else if (type == BlockEntityType.FURNACE) {
            return "Furnace";
        } else if (type == BlockEntityType.BLAST_FURNACE) {
            return "Blast Furnace";
        } else if (type == BlockEntityType.SMOKER) {
            return "Smoker";
        } else if (type == BlockEntityType.HOPPER) {
            return "Hopper";
        } else if (type == BlockEntityType.DISPENSER) {
            return "Dispenser";
        } else if (type == BlockEntityType.DROPPER) {
            return "Dropper";
        } else if (type == BlockEntityType.BREWING_STAND) {
            return "Brewing Stand";
        }
        return "Container";
    }

    /**
     * Loads items from NBT into an inventory.
     */
    private static void loadItemsFromNbt(Inventory inventory, CompoundTag nbt) {
        ListTag items = nbt.getListOrEmpty("Items");
        for (int i = 0; i < items.size(); i++) {
            items.getCompound(i).ifPresent(itemTag -> {
                int slot = itemTag.getByte("Slot").orElse((byte) 0) & 255;
                if (slot < inventory.getSize()) {
                    ItemStack item = itemFromNbt(itemTag);
                    if (item != null) {
                        inventory.setItem(slot, item);
                    }
                }
            });
        }
    }

    /**
     * Saves inventory items to NBT.
     */
    private static void saveItemsToNbt(Inventory inventory, CompoundTag nbt) {
        ListTag items = new ListTag();

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                CompoundTag itemTag = itemToNbt(item);
                itemTag.putByte("Slot", (byte) slot);
                items.add(itemTag);
            }
        }

        nbt.put("Items", items);
    }

    /**
     * Converts NBT to a Bukkit ItemStack using Codec-based deserialization.
     */
    private static ItemStack itemFromNbt(CompoundTag nbt) {
        try {
            net.minecraft.resources.RegistryOps<net.minecraft.nbt.Tag> registryOps =
                    net.minecraft.server.MinecraftServer.getServer().registryAccess()
                            .createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);

            return net.minecraft.world.item.ItemStack.CODEC
                    .parse(registryOps, nbt)
                    .resultOrPartial(error -> {})
                    .map(org.bukkit.craftbukkit.inventory.CraftItemStack::asBukkitCopy)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts a Bukkit ItemStack to NBT using Codec-based serialization.
     */
    private static CompoundTag itemToNbt(ItemStack item) {
        try {
            net.minecraft.world.item.ItemStack nmsItem = org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(item);

            net.minecraft.resources.RegistryOps<net.minecraft.nbt.Tag> registryOps =
                    net.minecraft.server.MinecraftServer.getServer().registryAccess()
                            .createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);

            return (CompoundTag) net.minecraft.world.item.ItemStack.CODEC
                    .encodeStart(registryOps, nmsItem)
                    .resultOrPartial(error -> {})
                    .orElse(new CompoundTag());
        } catch (Exception e) {
            return new CompoundTag();
        }
    }
}

