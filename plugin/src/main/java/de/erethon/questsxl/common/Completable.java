package de.erethon.questsxl.common;

import de.erethon.questsxl.common.QStage;
import de.erethon.questsxl.player.QPlayer;

import java.util.List;
import java.util.Set;

public interface Completable {

    void load();
    String getName();
    void reward(QPlayer player);
    void reward(Set<QPlayer> players);
    List<QStage> getStages();
}
