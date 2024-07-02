package de.erethon.questsxl.animation;

import org.bukkit.configuration.file.YamlConfiguration;

public class QAnimation {

    String id;

    public QAnimation(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void load(YamlConfiguration configuration) {

    }

    public YamlConfiguration save() {
        return new YamlConfiguration();
    }
}
