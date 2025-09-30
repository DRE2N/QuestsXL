package de.erethon.questsxl.gui;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.livingworld.CompletedExplorable;
import de.erethon.questsxl.livingworld.Explorable;
import de.erethon.questsxl.livingworld.Exploration;
import de.erethon.questsxl.livingworld.ExplorationSet;
import de.erethon.questsxl.livingworld.PlayerExplorer;
import de.erethon.questsxl.livingworld.explorables.LootChest;
import de.erethon.questsxl.livingworld.explorables.PointOfInterest;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.respawn.RespawnPoint;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ExplorationGUI implements InventoryHolder, Listener {

    private final QuestsXL plugin = QuestsXL.get();
    private final Player player;
    private final QPlayer qPlayer;
    private final PlayerExplorer explorer;
    private final Exploration exploration;

    private Inventory inventory;
    private ExplorationSet currentSet;
    private boolean showingSets;

    public ExplorationGUI(Player player) {
        this.player = player;
        this.qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        this.explorer = qPlayer.getExplorer();
        this.exploration = plugin.getExploration();
        this.showingSets = true;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        createInventory();
        populateWithSets();
    }

    private void createInventory() {
        Component title = Component.translatable("qxl.gui.exploration.title")
                .color(NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.BOLD, true);
        inventory = Bukkit.createInventory(this, 54, title);
    }

    private void populateWithSets() {
        inventory.clear();
        showingSets = true;
        currentSet = null;

        List<ExplorationSet> sets = new ArrayList<>(exploration.getSets());
        Location playerLocation = player.getLocation();

        // Sort by distance from player
        sets.sort(Comparator.comparingDouble(set ->
            set.averageLocation().distanceSquared(playerLocation)));

        for (int i = 0; i < Math.min(sets.size(), 45); i++) {
            ExplorationSet set = sets.get(i);
            ItemStack item = createSetItem(set);
            inventory.setItem(i, item);
        }
    }

    private ItemStack createSetItem(ExplorationSet set) {
        Material material = Material.MAP;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Set display name
        Component displayName = set.displayName().get()
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true);
        meta.displayName(displayName);

        // Calculate progress
        int total = set.entries().size();
        int completed = getCompletedCount(set);
        boolean isCompleted = completed >= total;

        // Calculate distance
        double distance = Math.sqrt(set.averageLocation().distanceSquared(player.getLocation()));

        // Create lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(set.description().get().color(NamedTextColor.GRAY));
        lore.add(Component.empty());

        if (isCompleted) {
            lore.add(Component.translatable("qxl.gui.exploration.set.completed"));
        }

        lore.add(Component.translatable("qxl.gui.exploration.set.progress",
                Component.text(completed), Component.text(total)));
        lore.add(Component.translatable("qxl.gui.exploration.set.distance",
                Component.text(Math.round(distance))));
        lore.add(Component.empty());
        lore.add(Component.translatable("qxl.gui.exploration.set.clicktoview")
                .color(NamedTextColor.YELLOW));

        meta.lore(lore);

        // Change material based on completion
        if (isCompleted) {
            item.setType(Material.FILLED_MAP);
        }

        item.setItemMeta(meta);
        return item;
    }

    private void populateWithExplorables(ExplorationSet set) {
        inventory.clear();
        showingSets = false;
        currentSet = set;

        // Add back button
        ItemStack backButton = createBackButton();
        inventory.setItem(45, backButton);

        List<Explorable> explorables = set.entries();
        for (int i = 0; i < Math.min(explorables.size(), 44); i++) {
            Explorable explorable = explorables.get(i);
            ItemStack item = createExplorableItem(explorable, set);
            inventory.setItem(i, item);
        }
    }

    private ItemStack createExplorableItem(Explorable explorable, ExplorationSet set) {
        Material material = getExplorableMaterial(explorable);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        boolean isDiscovered = isExplorableDiscovered(explorable, set);

        // Set display name
        Component displayName;
        if (isDiscovered) {
            displayName = explorable.displayName().get()
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true);
        } else {
            displayName = Component.translatable("qxl.explorable.undiscovered")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true);
        }
        meta.displayName(displayName);

        // Create lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        // Add description if available
        QTranslatable description = explorable.description();
        if (description != null) {
            lore.add(description.get().color(NamedTextColor.GRAY));
            lore.add(Component.empty());
        }
        if (isDiscovered) {
            lore.add(Component.translatable("qxl.gui.exploration.explorable.discovered"));
        } else {
            lore.add(Component.translatable("qxl.gui.exploration.explorable.undiscovered"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material getExplorableMaterial(Explorable explorable) {
        if (explorable instanceof PointOfInterest) {
            return Material.SPYGLASS;
        } else if (explorable instanceof LootChest) {
            return Material.CHEST;
        } else if (explorable instanceof RespawnPoint) {
            return Material.BEACON;
        }
        return Material.COMPASS;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        Component displayName = Component.translatable("qxl.gui.exploration.back")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true);
        meta.displayName(displayName);

        item.setItemMeta(meta);
        return item;
    }

    private int getCompletedCount(ExplorationSet set) {
        Set<CompletedExplorable> completed = explorer.getCompletedExplorables().get(set);
        if (completed == null) {
            return 0;
        }
        return (int) completed.stream()
                .filter(e -> set.entries().contains(e.explorable()))
                .count();
    }

    private boolean isExplorableDiscovered(Explorable explorable, ExplorationSet set) {
        Set<CompletedExplorable> completed = explorer.getCompletedExplorables().get(set);
        if (completed == null) {
            return false;
        }
        return completed.stream()
                .anyMatch(e -> e.explorable().equals(explorable));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != inventory) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (showingSets) {
            handleSetClick(event.getSlot());
        } else {
            handleExplorableClick(event.getSlot());
        }
    }

    private void handleSetClick(int slot) {
        List<ExplorationSet> sets = new ArrayList<>(exploration.getSets());
        Location playerLocation = player.getLocation();
        sets.sort(Comparator.comparingDouble(set ->
            set.averageLocation().distanceSquared(playerLocation)));

        if (slot >= 0 && slot < sets.size()) {
            ExplorationSet clickedSet = sets.get(slot);
            populateWithExplorables(clickedSet);
        }
    }

    private void handleExplorableClick(int slot) {
        if (slot == 45) { // Back button
            populateWithSets();
            return;
        }

        if (currentSet != null && slot >= 0 && slot < currentSet.entries().size()) {
            // Could add additional functionality here, like teleporting or showing more details
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void close() {
        player.closeInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
