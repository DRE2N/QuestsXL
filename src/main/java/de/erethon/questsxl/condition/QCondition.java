package de.erethon.questsxl.condition;

import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public interface QCondition {

    boolean check(QPlayer player);
    boolean check(QEvent event);
    boolean fail(QPlayer player);
    boolean fail(QEvent event);
    boolean success(QPlayer player);
    boolean success(QEvent event);

    String getDisplayText();
    void load(ConfigurationSection cfg);
    void load(String[] msg);
}
