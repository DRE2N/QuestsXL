package de.erethon.questsxl.quest;

import de.erethon.questsxl.players.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.List;
import java.util.Set;

public interface Completable {

    void load();
    String getName();
    void reward(QPlayer player);
    void reward(Set<QPlayer> players);
    List<QStage> getStages();
}
