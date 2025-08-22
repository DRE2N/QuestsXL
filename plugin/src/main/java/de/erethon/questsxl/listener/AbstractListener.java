package de.erethon.questsxl.listener;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.data.QDatabaseManager;
import de.erethon.questsxl.livingworld.QEventManager;
import org.bukkit.event.Listener;

/**
 * @author Fyreum
 */
public abstract class AbstractListener implements Listener {

    QDatabaseManager databaseManager = QuestsXL.get().getDatabaseManager();

    QEventManager eventManager = QuestsXL.get().getEventManager();
}
