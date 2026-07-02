package de.erethon.questsxl.livingworld.explorables;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemStack;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.QLineConfig;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.common.script.QTranslatableConfig;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.Explorable;
import de.erethon.questsxl.livingworld.ExplorationSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LootChest implements Explorable {

    private String id;
    private QTranslatable displayName;
    private QTranslatable description;
    private ExplorationSet set;
    private Location chestLocation;
    private List<ItemStack> lootItems = new ArrayList<>();
    private List<HermesLootItem> hermesLootItems = new ArrayList<>();

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

    public List<HermesLootItem> getHermesLootItems() {
        return new ArrayList<>(hermesLootItems);
    }

    public void setLootItems(List<ItemStack> lootItems) {
        this.lootItems = new ArrayList<>(lootItems);
    }

    public void setHermesLootItems(List<HermesLootItem> hermesLootItems) {
        this.hermesLootItems = new ArrayList<>(hermesLootItems);
    }

    public void addLootItem(ItemStack item) {
        this.lootItems.add(item);
    }

    public void addHermesLootItem(HermesLootItem item) {
        this.hermesLootItems.add(item);
    }

    public static LootChest fromQLineConfig(QLineConfig section) {
        try {
            LootChest chest = new LootChest();
            chest.id = section.getName();
            chest.set = QuestsXL.get().getExploration().getSet(section.getString("parentSet"));
            if (chest.set == null) {
                throw new IllegalArgumentException("Parent set not found");
            }
            chest.displayName = QTranslatableConfig.fromQLine(section, "displayName", "qxl.lootchest." + chest.id + ".displayName", "<missing translation>");
            chest.description = QTranslatableConfig.fromQLine(section, "description", "qxl.lootchest." + chest.id + ".description", "<missing translation>");

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

            String hermesLootString = section.getString("lootItems", "");
            if (!hermesLootString.isEmpty()) {
                for (HermesLootItem item : parseHermesLootItems(hermesLootString)) {
                    chest.addHermesLootItem(item);
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

    private static List<HermesLootItem> parseHermesLootItems(String input) {
        List<HermesLootItem> result = new ArrayList<>();
        for (String part : input.split(",")) {
            String entry = part.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int separator = entry.lastIndexOf(':');
            String item = entry;
            int chance = 100;
            if (separator > 0 && separator < entry.length() - 1) {
                String possibleChance = entry.substring(separator + 1);
                try {
                    chance = Integer.parseInt(possibleChance);
                    item = entry.substring(0, separator);
                } catch (NumberFormatException ignored) {
                    chance = 100;
                }
            }
            result.add(new HermesLootItem(item, chance));
        }
        return result;
    }

    private static String hermesLootItemsToString(List<HermesLootItem> items) {
        List<String> result = new ArrayList<>();
        for (HermesLootItem item : items) {
            if (item.itemId() == null || item.itemId().isBlank()) {
                continue;
            }
            result.add(item.itemId() + ":" + clampChance(item.chance()));
        }
        return String.join(",", result);
    }

    private static int clampChance(int chance) {
        return Math.max(0, Math.min(100, chance));
    }

    public List<ItemStack> rollHermesLootItems() {
        List<ItemStack> result = new ArrayList<>();
        if (!QuestsXL.get().isHephaestusEnabled()) {
            return result;
        }
        for (HermesLootItem lootItem : hermesLootItems) {
            if (ThreadLocalRandom.current().nextInt(100) >= clampChance(lootItem.chance())) {
                continue;
            }
            NamespacedKey key = NamespacedKey.fromString(lootItem.itemId());
            if (key == null) {
                continue;
            }
            HItem item = QuestsXL.get().getItemLibrary().get(key);
            if (item == null) {
                continue;
            }
            HItemStack stack = item.rollRandomStack();
            if (stack != null && stack.getBukkitStack() != null && stack.getBukkitStack().getType() != Material.AIR) {
                result.add(stack.getBukkitStack().clone());
            }
        }
        return result;
    }

    public QLineConfig toQLineConfig() {
        QLineConfig cfg = new QLineConfig();
        cfg.setName(id); // Set the ID as the name
        cfg.set("parentSet", set != null ? set.id() : "");
        QTranslatableConfig.toQLine(cfg, "displayName", displayName);
        QTranslatableConfig.toQLine(cfg, "description", description);
        cfg.set("location.x", chestLocation.getX());
        cfg.set("location.y", chestLocation.getY());
        cfg.set("location.z", chestLocation.getZ());
        cfg.set("location.world", chestLocation.getWorld().getName());
        if (!lootItems.isEmpty()) {
            cfg.set("loot", toString(lootItems.toArray(new ItemStack[0])));
        }
        if (!hermesLootItems.isEmpty()) {
            cfg.set("lootItems", hermesLootItemsToString(hermesLootItems));
        }

        return cfg;
    }

    public record HermesLootItem(String itemId, int chance) {
    }
}
