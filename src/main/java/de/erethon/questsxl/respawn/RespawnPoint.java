package de.erethon.questsxl.respawn;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

enum UnlockMode {
    NEAR,
    ACTION,
    QUEST
}
enum UseMode {
    NEAREST,
    LAST,
}

public class RespawnPoint {

    QuestsXL plugin = QuestsXL.getInstance();

    String id;
    Location location;
    String displayName;
    int cooldown;
    long lastUsed;
    UnlockMode unlockMode;
    UseMode useMode;
    String useQuest;

    public RespawnPoint(String id) {
        this.id = id;
    }

    public RespawnPoint(String id, Location location) {
        this.id = id;
        this.location = location;
    }

    public void setLastUsed() {
        lastUsed = System.currentTimeMillis();
    }

    public boolean canRespawn(QPlayer qPlayer) {
        long now = System.currentTimeMillis();
        if (cooldown != 0 && lastUsed + cooldown < now) {
            return false;
        }
        if (unlockMode == UnlockMode.QUEST && useQuest != null) {
            QQuest quest = plugin.getQuestManager().getByName(useQuest);
            if (quest == null) {
                return false;
            }
            return qPlayer.hasQuest(quest);
        }
        return true;
    }

    public String getId() {
        return id;
    }

    public ConfigurationSection save() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("location", location);
        configuration.set("displayName", displayName);
        configuration.set("cooldown", cooldown);
        configuration.set("unlockMode", unlockMode);
        configuration.set("useMode", useMode);
        configuration.set("quest", useQuest);
        return configuration;
    }

    public void load(ConfigurationSection section) {
        location = section.getLocation("location");
        displayName = section.getString("displayName", null);
        cooldown = section.getInt("cooldown", 0);
        unlockMode = UnlockMode.valueOf(section.getString("unlockMode", "NEAR"));
        useMode = UseMode.valueOf(section.getString("useMode", "NEAREST"));
        useQuest = section.getString("quest", null);
    }
}
