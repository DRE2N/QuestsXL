package de.erethon.questsxl.listener;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.QEventManager;
import de.erethon.questsxl.player.QPlayerCache;
import org.bukkit.event.Listener;

/**
 * @author Fyreum
 */
public abstract class AbstractListener implements Listener {

    QPlayerCache cache = QuestsXL.get().getPlayerCache();
    QEventManager eventManager = QuestsXL.get().getEventManager();
}
