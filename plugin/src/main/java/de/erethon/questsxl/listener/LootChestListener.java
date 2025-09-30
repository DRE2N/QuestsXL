package de.erethon.questsxl.listener;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.LootChestManager;
import de.erethon.questsxl.livingworld.explorables.LootChest;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

public class LootChestListener extends AbstractListener {

    private final QuestsXL plugin;
    private final LootChestManager lootChestManager;

    public LootChestListener(QuestsXL plugin) {
        this.plugin = plugin;
        this.lootChestManager = plugin.getLootChestManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        if (clickedBlock.getType() != Material.CHEST &&
            clickedBlock.getType() != Material.TRAPPED_CHEST) {
            return;
        }

        String chestId = lootChestManager.getChestId(clickedBlock);
        if (chestId == null) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        if (qPlayer == null) {
            return;
        }

        LootChest lootChest = lootChestManager.getLootChest(chestId);
        if (lootChest == null) {
            plugin.debug("Loot chest configuration not found for ID: " + chestId);
            return;
        }

        lootChestManager.openLootChest(qPlayer, lootChest);
    }

}
