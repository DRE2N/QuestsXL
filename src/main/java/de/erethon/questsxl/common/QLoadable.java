package de.erethon.questsxl.common;

import org.bukkit.configuration.ConfigurationSection;

public interface QLoadable {

    void load(ConfigurationSection section);
    void load(QLineConfig section);
}
