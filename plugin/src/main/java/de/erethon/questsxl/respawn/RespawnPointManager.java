package de.erethon.questsxl.respawn;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.explorables.ExplorableRespawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RespawnPointManager implements Listener {

    QuestsXL plugin = QuestsXL.get();

    List<RespawnPoint> points = new ArrayList<>();
    Map<String, ExplorableRespawnPoint> explorableRespawnPoints = new HashMap<>();

    public RespawnPointManager(File file) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        load(file);
    }

    public void load(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            RespawnPoint point = new RespawnPoint(key);
            point.load(config.getConfigurationSection(key));
            points.add(point);
        }

        // Create explorable respawn points for NEAR mode points after loading
        updateExplorationIntegration();
    }

    public void save() {
        YamlConfiguration configuration = new YamlConfiguration();
        for (RespawnPoint point : points) {
            configuration.set(String.valueOf(point.getId()), point.save());
        }
        try {
            configuration.save(QuestsXL.RESPAWNS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addRespawnPoint(RespawnPoint point) {
        if (point != null) {
            points.add(point);

            // If this is a NEAR unlock mode respawn point, add it to the exploration system
            if (point.getUnlockMode() == RespawnPointUnlockMode.NEAR) {
                ExplorableRespawnPoint explorable = new ExplorableRespawnPoint(point);
                explorableRespawnPoints.put(point.getId(), explorable);
                plugin.getExploration().addExplorableRespawnPoint(explorable);
            }
        }
    }

    public void removeRespawnPoint(RespawnPoint point) {
        if (point != null) {
            points.remove(point);

            // Remove from exploration system if it exists
            ExplorableRespawnPoint explorable = explorableRespawnPoints.get(point.getId());
            if (explorable != null) {
                plugin.getExploration().removeExplorableRespawnPoint(explorable);
                explorableRespawnPoints.remove(point.getId());
            }
        }
    }

    public RespawnPoint getRespawnPoint(String id) {
        for (RespawnPoint point : points) {
            if (point.getId().equals(id)) {
                return point;
            }
        }
        return null;
    }

    public List<RespawnPoint> getRespawnPoints() {
        return new ArrayList<>(points);
    }

    public ExplorableRespawnPoint getExplorableRespawnPoint(String id) {
        return explorableRespawnPoints.get(id);
    }

    /**
     * Gets the best respawn point for a player based on their death location.
     * Rules:
     * 1. If the last used respawn point has UseMode.LAST, use that
     * 2. Otherwise, use the nearest unlocked respawn point to the death location
     *
     * @param qPlayer        The player who died
     * @param deathLocation The location where the player died
     * @return The best respawn point, or null if no unlocked respawn points are available
     */
    public RespawnPoint getBestRespawnPoint(de.erethon.questsxl.player.QPlayer qPlayer, org.bukkit.Location deathLocation) {
        RespawnPoint lastUsed = qPlayer.getExplorer().getLastRespawnPoint();
        if (lastUsed != null && lastUsed.getUseMode() == UseMode.LAST &&
                qPlayer.getExplorer().isRespawnPointUnlocked(lastUsed) &&
                lastUsed.canRespawn(qPlayer)) {
            return lastUsed;
        }

        RespawnPoint nearestRespawnPoint = null;
        double nearestDistance = Double.MAX_VALUE;

        for (RespawnPoint respawnPoint : points) {
            if (!qPlayer.getExplorer().isRespawnPointUnlocked(respawnPoint) || !respawnPoint.canRespawn(qPlayer)) {
                continue;
            }

            double distance = deathLocation.distance(respawnPoint.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestRespawnPoint = respawnPoint;
            }
        }

        return nearestRespawnPoint;
    }

    // We need to register NEAR points as explorable respawn points
    private void updateExplorationIntegration() {
        for (ExplorableRespawnPoint explorable : explorableRespawnPoints.values()) {
            plugin.getExploration().removeExplorableRespawnPoint(explorable);
        }
        explorableRespawnPoints.clear();

        for (RespawnPoint point : points) {
            if (point.getUnlockMode() == RespawnPointUnlockMode.NEAR) {
                ExplorableRespawnPoint explorable = new ExplorableRespawnPoint(point);
                explorableRespawnPoints.put(point.getId(), explorable);
                plugin.getExploration().addExplorableRespawnPoint(explorable);
            }
        }
    }
}
