package de.erethon.questsxl.livingworld;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.explorables.LootChest;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.TileState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootChestManager {

    private final QuestsXL plugin;
    private final NamespacedKey lootChestKey;
    private final Map<String, LootChest> lootChests = new HashMap<>();

    public LootChestManager(QuestsXL plugin) {
        this.plugin = plugin;
        this.lootChestKey = new NamespacedKey(plugin, "loot_chest_id");

        plugin.getDatabaseManager().getPlayerDao().createLootChestTable();

        loadLootChests();
    }

    private void loadLootChests() {
        Exploration exploration = plugin.getExploration();
        if (exploration != null) {
            for (ExplorationSet set : exploration.getSets()) {
                for (Explorable exp : set.getExplorables()) {
                    if (exp instanceof LootChest chest) {
                        lootChests.put(chest.id(), chest);
                    }
                }
            }
        }
    }

    public LootChest createLootChest(String chestId, Block chestBlock, ExplorationSet parentSet) {
        if (chestBlock.getType() != Material.CHEST && chestBlock.getType() != Material.TRAPPED_CHEST) {
            return null;
        }

        Chest chestState = (Chest) chestBlock.getState();

        LootChest lootChest = new LootChest(chestId, chestBlock.getLocation());
        lootChest.setSet(parentSet);

        List<ItemStack> lootItems = new ArrayList<>();
        for (ItemStack item : chestState.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                lootItems.add(item.clone());
            }
        }
        lootChest.setLootItems(lootItems);
        lootChests.put(lootChest.id(), lootChest);
        PersistentDataContainer pdc = chestState.getPersistentDataContainer();
        pdc.set(lootChestKey, PersistentDataType.STRING, lootChest.id());
        plugin.debug("Registered loot chest " + lootChest.id() + " at " + chestState.getLocation());
        plugin.debug("Items in chest: " + lootChest.getLootItems().size());
        plugin.debug("PDC value set: " + pdc.get(lootChestKey, PersistentDataType.STRING));

        chestState.getInventory().clear();
        chestState.update(true);

        return lootChest;
    }

    public String getChestId(Block block) {
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) {
            return null;
        }

        Chest chestState = (Chest) block.getState();
        PersistentDataContainer pdc = chestState.getPersistentDataContainer();
        return pdc.get(lootChestKey, PersistentDataType.STRING);
    }

    public LootChest getLootChest(String chestId) {
        return lootChests.get(chestId);
    }

    public boolean isLootChest(Block block) {
        return getChestId(block) != null;
    }

    public boolean hasPlayerLootedChest(QPlayer player, String chestId) {
        return plugin.getDatabaseManager().getPlayerDao().hasLootedChest(player.getUUID(), chestId);
    }

    public void openLootChest(QPlayer qPlayer, LootChest lootChest) {
        if (hasPlayerLootedChest(qPlayer, lootChest.id())) {
            plugin.debug("Player " + qPlayer.getPlayer().getName() + " tried to loot already looted chest " + lootChest.id());
            return;
        }

        dropLootNicely(lootChest, qPlayer.getPlayer());

        QuestsXL.get().getDatabaseManager().getPlayerDao().markChestLooted(
                qPlayer.getUUID(),
                lootChest.id(),
                System.currentTimeMillis()
        );
        qPlayer.getExplorer().completeExplorable(lootChest.set(), lootChest, System.currentTimeMillis());
        qPlayer.getPlayer().playSound(lootChest.location(), Sound.ENTITY_PLAYER_LEVELUP, 0.66f, 2);

        QuestsXL.get().debug("Player " + qPlayer.getPlayer().getName() + " looted chest " + lootChest.id());
    }

    private void dropLootNicely(LootChest lootChest, Player player) {
        for (ItemStack item : lootChest.getLootItems()) {
            Item droppedItem = lootChest.location().getWorld().dropItemNaturally(lootChest.location().clone().add(0, 1, 0), item, item1 -> {
                item1.setVelocity(item1.getVelocity().multiply(0.5));
                item1.setPickupDelay(0);
                item1.setCanMobPickup(false);
                item1.setVisibleByDefault(false);
                item1.setOwner(player.getUniqueId());
                item1.setWillAge(false);
            });
            player.showEntity(QuestsXL.get(), droppedItem);
        }
    }

    public void reloadLootChests() {
        lootChests.clear();
        loadLootChests();
    }

    public NamespacedKey getLootChestKey() {
        return lootChestKey;
    }

    public Map<String, LootChest> getAllLootChests() {
        return new HashMap<>(lootChests);
    }
}
