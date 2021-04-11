package de.erethon.questsxl.listener;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.objectives.ActiveObjective;
import de.erethon.questsxl.players.QPlayerCache;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerListener implements Listener {

    QPlayerCache cache = QuestsXL.getInstance().getPlayerCache();

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        for (ActiveObjective objective : cache.get(event.getPlayer()).getCurrentObjectives()) {
            objective.check(event);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        for (ActiveObjective objective : cache.get(event.getPlayer()).getCurrentObjectives()) {
            objective.check(event);
        }
    }
}
