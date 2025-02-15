package de.erethon.questsxl.common;

import de.erethon.questsxl.player.QPlayer;

import java.util.List;
import java.util.Set;

/**
 * Represents something that has a variety of stages that need to be completed to finish it.
 * Quests, Events and global objectives are all completable.
 */
public interface Completable {

    void load();
    String getName();
    void reward(QPlayer player);
    void reward(Set<QPlayer> players);
    List<QStage> getStages();
}
