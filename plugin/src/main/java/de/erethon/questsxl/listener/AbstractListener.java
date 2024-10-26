package de.erethon.questsxl.listener;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.livingworld.QEventManager;
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
    QEventManager eventManager = QuestsXL.getInstance().getEventManager();

    void checkObjectives(Player player, Event event) {
        for (ActiveObjective objective : cache.getByPlayer(player).getCurrentObjectives()) {
            objective.check(event);
        }
        for (QEvent qEvent : eventManager.getActiveEvents()) {
            for (ActiveObjective objective : qEvent.getCurrentObjectives()) {
                objective.check(event);
            }
        }
    }
}
