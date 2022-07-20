package de.erethon.questsxl.action;

import de.erethon.questsxl.livingworld.QEvent;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public interface QAction {

    void play(Player player);
    void play(QEvent event);
    void onFinish(Player player);
    void onFinish(QEvent event);

    boolean conditions(Player player);

    void delayedEnd(int seconds);

    void cancel();

    Material getIcon();

    String getID();

    void load(ConfigurationSection cfg);
    void load(String[] msg);

}
