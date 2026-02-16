package de.erethon.questsxl.instancing;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener for inventory events related to instanced block entities.
 * Ensures inventory changes are synced to NBT when players close containers.
 */
public class InstanceInventoryListener implements Listener {

    private final QuestsXL plugin;

    public InstanceInventoryListener(QuestsXL plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        // Check if this is a virtual inventory from our instancing system
        if (holder instanceof VirtualInventoryHolder virtualHolder) {
            VirtualBlockEntity entity = virtualHolder.getEntity();

            QuestsXL.log("[InstanceInventory] Player closed virtual inventory at " + entity.getPosition());

            // Sync the inventory contents to NBT
            entity.syncInventoryToData();

            QuestsXL.log("[InstanceInventory] Synced inventory to NBT");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
            if (qPlayer != null) {
                updatePlayerVisibilityForJoin(qPlayer);
            }
        }, 20L);
    }


    /**
     * Updates visibility for a player who just joined or changed worlds.
     * If they're in an instance, hide other players not in the same instance.
     * If they're not in an instance, make sure they can see everyone.
     */
    private void updatePlayerVisibilityForJoin(QPlayer qPlayer) {
        InstanceManager manager = plugin.getInstanceManager();
        if (manager == null) {
            return;
        }

        Player player = qPlayer.getPlayer();
        InstancedArea playerInstance = manager.getActiveInstance(qPlayer);

        // Check all players in the same world
        for (Player otherPlayer : player.getWorld().getPlayers()) {
            if (otherPlayer.equals(player)) {
                continue; // Skip self
            }

            QPlayer otherQPlayer = plugin.getDatabaseManager().getCurrentPlayer(otherPlayer);
            if (otherQPlayer == null) {
                continue;
            }

            InstancedArea otherInstance = manager.getActiveInstance(otherQPlayer);

            // Determine visibility based on instance membership
            if (playerInstance != null && otherInstance != null && playerInstance.equals(otherInstance)) {
                // Both in same instance - show each other
                player.showEntity(plugin, otherPlayer);
                otherPlayer.showEntity(plugin, player);
            } else if (playerInstance != null || otherInstance != null) {
                // One or both in an instance, but not the same - hide each other
                player.hideEntity(plugin, otherPlayer);
                otherPlayer.hideEntity(plugin, player);
            } else {
                // Neither in an instance - show each other
                player.showEntity(plugin, otherPlayer);
                otherPlayer.showEntity(plugin, player);
            }
        }
    }
}

