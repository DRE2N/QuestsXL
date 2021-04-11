package de.erethon.questsxl.action;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;

public interface QAction {

    void play(Player player);

    boolean conditions(Player player);

    void delayedEnd(int seconds);

    void cancel();

    Material getIcon();

    String getID();

    void load(ConfigurationSection cfg);
    void load(String[] msg);

}
