package de.erethon.questsxl.condition;

import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public interface QCondition {

    boolean check(QPlayer player);
    boolean fail(QPlayer player);
    boolean success(QPlayer player);

    String getDisplayText();
    void load(ConfigurationSection cfg);
    void load(String[] msg);
}
