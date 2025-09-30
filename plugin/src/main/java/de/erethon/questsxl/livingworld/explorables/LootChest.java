package de.erethon.questsxl.livingworld.explorables;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.Explorable;
import de.erethon.questsxl.livingworld.ExplorationSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class LootChest implements Explorable {

    private String id;
    private QTranslatable displayName;
    private QTranslatable description;
    private ExplorationSet set;
    private Location chestLocation;
    private List<ItemStack> lootItems = new ArrayList<>();

    public LootChest() {
        // Default constructor for QLineConfig parsing
    }

    public LootChest(String id, Location location) {
        this.id = id;
        this.chestLocation = location;
        this.displayName = QTranslatable.fromString("<missing translation>");
        this.description = QTranslatable.fromString("<missing translation>");
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public QTranslatable displayName() {
        return displayName;
    }

    @Override
    public Location location() {
        return chestLocation;
    }

    @Override
    public QTranslatable description() {
        return description;
    }

    @Override
    public boolean countsForProgress() {
        return false; // Loot chests typically don't count for exploration progress
    }

    public void setSet(ExplorationSet set) {
        this.set = set;
    }

    public ExplorationSet set() {
        return set;
    }

    public void setDisplayName(QTranslatable displayName) {
        this.displayName = displayName;
    }

    public void setDescription(QTranslatable description) {
        this.description = description;
    }

    public List<ItemStack> getLootItems() {
        return new ArrayList<>(lootItems);
    }

    public void setLootItems(List<ItemStack> lootItems) {
        this.lootItems = new ArrayList<>(lootItems);
    }

    public void addLootItem(ItemStack item) {
        this.lootItems.add(item);
    }

    public static LootChest fromQLineConfig(QLineConfig section) {
        try {
            LootChest chest = new LootChest();
            chest.id = section.getName();
            chest.set = QuestsXL.get().getExploration().getSet(section.getString("parentSet"));
            if (chest.set == null) {
                throw new IllegalArgumentException("Parent set not found");
            }
            chest.displayName = QTranslatable.fromString(section.getString("displayName", "<missing translation>"));
            chest.description = QTranslatable.fromString(section.getString("description", "<missing translation>"));

            double x = section.getDouble("location.x");
            double y = section.getDouble("location.y");
            double z = section.getDouble("location.z");
            World world = Bukkit.getWorld(section.getString("location.world", "Erethon"));
            chest.chestLocation = new Location(world, x, y, z);

            String lootString = section.getString("loot", "");
            if (!lootString.isEmpty()) {
                ItemStack[] items = fromString(lootString);
                for (ItemStack item : items) {
                    if (item != null && item.getType() != Material.AIR) {
                        chest.addLootItem(item);
                    }
                }
            }

            return chest;
        } catch (Exception e) {
            FriendlyError error = new FriendlyError("LootChest" + section.getName(), "Error while parsing from QLineConfig", e.getMessage(), "Check the configuration for errors");
            error.addStacktrace(e.getStackTrace());
            QuestsXL.get().addRuntimeError(error);
            return null;
        }
    }

    private static ItemStack[] fromString(String input) {
        byte[] decoded = Base64.getDecoder().decode(input);
        return ItemStack.deserializeItemsFromBytes(decoded);
    }

    private static String toString(ItemStack[] items) {
        byte[] serialized = ItemStack.serializeItemsAsBytes(items);
        return Base64.getEncoder().encodeToString(serialized);
    }

    public QLineConfig toQLineConfig() {
        QLineConfig cfg = new QLineConfig();
        cfg.setName(id); // Set the ID as the name
        cfg.set("parentSet", set != null ? set.id() : "");
        cfg.set("displayName", displayName.toString());
        cfg.set("description", description.toString());
        cfg.set("location.x", chestLocation.getX());
        cfg.set("location.y", chestLocation.getY());
        cfg.set("location.z", chestLocation.getZ());
        cfg.set("location.world", chestLocation.getWorld().getName());
        cfg.set("loot", toString(lootItems.toArray(new ItemStack[0])));

        return cfg;
    }
}
