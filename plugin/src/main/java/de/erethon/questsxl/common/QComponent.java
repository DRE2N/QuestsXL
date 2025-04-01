package de.erethon.questsxl.common;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;

/**
 * A QComponent is a part of the QuestsXL plugin that can be loaded and unloaded from a QConfig.
 * QComponents can be nested and usually have a parent component.
 */
public interface QComponent extends ContextAware {

     /**
     * Get a player ObjectiveHolder from the player cache.
     * @param player the Bukkit player to get the ObjectiveHolder for
     * @return the ObjectiveHolder for the player
     */
    default QPlayer getPlayerHolder(Player player) {
        return QuestsXL.getInstance().getPlayerCache().getByPlayer(player);
    }

    default void load(QConfig cfg) {
        //
    }
}
