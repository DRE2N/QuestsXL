package de.erethon.questsxl.condition;

import de.erethon.questsxl.players.QPlayer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public interface QCondition {

    boolean check(QPlayer player);

    String getDisplayText();
    void load(ConfigurationSection cfg);
    void load(String[] msg);
}
