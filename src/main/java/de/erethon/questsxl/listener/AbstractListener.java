package de.erethon.questsxl.listener;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.player.QPlayerCache;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

/**
 * @author Fyreum
 */
public abstract class AbstractListener implements Listener {

    QPlayerCache cache = QuestsXL.getInstance().getPlayerCache();

    void checkObjectives(Player player, Event event) {
        for (ActiveObjective objective : cache.get(player).getCurrentObjectives()) {
            objective.check(event);
        }
    }

}
