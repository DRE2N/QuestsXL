package de.erethon.questsxl.respawn;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.ExplorationSet;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RespawnPointManager implements Listener {

    QuestsXL plugin = QuestsXL.get();

    List<RespawnPoint> points = new ArrayList<>();

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

            // If this is an explorable respawn point (NEAR or ACTION mode), add it to exploration system
            if (point.getUnlockMode() == RespawnPointUnlockMode.NEAR ||
                    point.getUnlockMode() == RespawnPointUnlockMode.ACTION) {
                addToExplorationSystem(point);
            }
        }
    }

    /**
     * Adds a respawn point to the appropriate exploration system based on its configuration.
     * NEAR and ACTION mode respawn points become part of the exploration system.
     */
    private void addToExplorationSystem(RespawnPoint point) {
        // For now, add as standalone explorables. In the future, we can assign them to specific exploration sets
        // based on their location or configuration
        ExplorationSet closestSet = plugin.getExploration().getClosestSet(point.location());
        if (closestSet != null && shouldAddToSet(point, closestSet)) {
            closestSet.entries().add(point);
        } else {
            // Add as a standalone explorable that players can discover
            // This ensures they appear in the Content Guide and can be unlocked by proximity
            plugin.getExploration().addStandaloneExplorable(point);
        }
    }

    /**
     * Determines if a respawn point should be added to a specific exploration set.
     * This can be extended with more sophisticated logic later.
     */
    private boolean shouldAddToSet(RespawnPoint point, ExplorationSet set) {
        // Simple distance-based check - if respawn point is within reasonable distance of the set
        if (set.averageLocation() != null && point.location() != null) {
            double distance = set.averageLocation().distance(point.location());
            return distance <= 100; // Within 100 blocks
        }
        return false;
    }

    /**
     * Removes a respawn point from the exploration system
     */
    private void removeFromExplorationSystem(RespawnPoint point) {
        // Remove from any exploration sets
        for (ExplorationSet set : plugin.getExploration().getSets()) {
            set.entries().remove(point);
        }

        // Remove from standalone explorables
        plugin.getExploration().removeStandaloneExplorable(point);
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

            // If this is an explorable respawn point, add it to the exploration system
            if (point.getUnlockMode() == RespawnPointUnlockMode.NEAR ||
                    point.getUnlockMode() == RespawnPointUnlockMode.ACTION) {
                addToExplorationSystem(point);
            }
        }
    }

    public void removeRespawnPoint(RespawnPoint point) {
        if (point != null) {
            points.remove(point);

            // Remove from exploration system if it was explorable
            if (point.getUnlockMode() == RespawnPointUnlockMode.NEAR ||
                point.getUnlockMode() == RespawnPointUnlockMode.ACTION) {
                removeFromExplorationSystem(point);
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
}
