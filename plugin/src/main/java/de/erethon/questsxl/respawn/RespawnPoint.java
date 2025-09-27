package de.erethon.questsxl.respawn;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

enum UseMode {
    NEAREST,
    LAST,
}

public class RespawnPoint {

    QuestsXL plugin = QuestsXL.get();

    String id;
    Location location;
    QTranslatable displayName;
    int cooldown;
    long lastUsed;
    RespawnPointUnlockMode respawnPointUnlockMode;
    UseMode useMode;
    String useQuest;

    public RespawnPoint(String id) {
        this.id = id;
    }

    public RespawnPoint(String id, Location location) {
        this.id = id;
        Location l = location.clone();
        l.setPitch(0);
        l.setYaw(0);
        l.add(0, 1.5f, 0);
        this.location = l;
    }

    public void setLastUsed() {
        lastUsed = System.currentTimeMillis();
    }

    public boolean canRespawn(QPlayer qPlayer) {
        long now = System.currentTimeMillis();
        if (cooldown != 0 && lastUsed + cooldown > now) {
            return false;
        }
        if (respawnPointUnlockMode == RespawnPointUnlockMode.QUEST && useQuest != null) {
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

    public Location getLocation() {
        return location;
    }

    public QTranslatable getDisplayName() {
        return displayName;
    }

    public UseMode getUseMode() {
        return useMode;
    }

    public String getUseQuest() {
        return useQuest;
    }

    public RespawnPointUnlockMode getUnlockMode() {
        return respawnPointUnlockMode;
    }

    public void setDisplayName(QTranslatable displayName) {
        this.displayName = displayName;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setUseMode(UseMode useMode) {
        this.useMode = useMode;
    }

    public void setUseQuest(String useQuest) {
        this.useQuest = useQuest;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public void setUnlockMode(RespawnPointUnlockMode respawnPointUnlockMode) {
        this.respawnPointUnlockMode = respawnPointUnlockMode;
    }

    public ConfigurationSection save() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("location", location);
        configuration.set("displayName", displayName.toString());
        configuration.set("cooldown", cooldown);
        configuration.set("unlockMode", respawnPointUnlockMode.name());
        if (useMode == null) {
            useMode = UseMode.NEAREST;
        }
        configuration.set("useMode", useMode.name());
        configuration.set("quest", useQuest);
        return configuration;
    }

    public void load(ConfigurationSection section) {
        location = section.getLocation("location");
        displayName = QTranslatable.fromString(section.getString("displayName"));
        cooldown = section.getInt("cooldown", 0);
        respawnPointUnlockMode = RespawnPointUnlockMode.valueOf(section.getString("unlockMode", "NEAR"));
        useMode = UseMode.valueOf(section.getString("useMode", "NEAREST"));
        useQuest = section.getString("quest", null);
    }
}
