package de.erethon.questsxl.livingworld;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.explorables.LootChest;
import de.erethon.questsxl.livingworld.explorables.PointOfInterest;
import de.erethon.questsxl.respawn.RespawnPoint;
import de.erethon.questsxl.respawn.RespawnPointUnlockMode;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager for saving and loading Explorables and ExplorationSets to/from YAML files.
 * This handles persistence for Points of Interest, Loot Chests, and Exploration Sets,
 * including their hierarchical relationships.
 */
public class ExplorationManager {

    private final QuestsXL plugin = QuestsXL.get();
    private final Exploration exploration;

    private final File explorablesFile;
    private final File explorationSetsFile;

    public ExplorationManager(File explorablesFile, File explorationSetsFile) {
        this.exploration = plugin.getExploration();
        this.explorablesFile = explorablesFile;
        this.explorationSetsFile = explorationSetsFile;
        load();
    }

    /**
     * Loads all exploration data from YAML files.
     * First loads ExplorationSets, then Explorables that reference them.
     */
    public void load() {
        // Clear existing data to handle reloads properly
        exploration.clearAll();

        loadExplorationSets();
        loadExplorables();
        establishSetHierarchy();

        // After loading exploration data, ensure respawn points are properly added to the exploration system
        reloadRespawnPointsInExploration();
    }

    /**
     * Re-adds all explorable respawn points to the exploration system.
     * This is necessary after clearAll() removes them during reload.
     */
    private void reloadRespawnPointsInExploration() {
        QuestsXL plugin = QuestsXL.get();
        if (plugin.getRespawnPointManager() == null) {
            return;
        }

        for (RespawnPoint respawnPoint : plugin.getRespawnPointManager().getRespawnPoints()) {
            // Only add respawn points that should be explorable (NEAR or ACTION mode)
            if (respawnPoint.getUnlockMode() == RespawnPointUnlockMode.NEAR ||
                respawnPoint.getUnlockMode() == RespawnPointUnlockMode.ACTION) {

                // Check if it should be added to an exploration set or as standalone
                ExplorationSet closestSet = exploration.getClosestSet(respawnPoint.location());
                if (closestSet != null && shouldAddToSet(respawnPoint, closestSet)) {
                    closestSet.entries().add(respawnPoint);
                } else {
                    // Add as standalone explorable
                    exploration.addStandaloneExplorable(respawnPoint);
                }
            }
        }
    }

    /**
     * Determines if a respawn point should be added to a specific exploration set.
     */
    private boolean shouldAddToSet(RespawnPoint respawnPoint, ExplorationSet set) {
        // Simple distance-based check - if respawn point is within reasonable distance of the set
        if (set.averageLocation() != null && respawnPoint.location() != null) {
            double distance = set.averageLocation().distance(respawnPoint.location());
            return distance <= 100; // Within 100 blocks
        }
        return false;
    }

    /**
     * Loads ExplorationSets from the sets YAML file.
     */
    private void loadExplorationSets() {
        if (!explorationSetsFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(explorationSetsFile);
        List<String> setConfigs = config.getStringList("sets");

        for (String setConfigString : setConfigs) {
            QLineConfig qLineConfig = new QLineConfig(setConfigString);
            ExplorationSet set = ExplorationSet.fromQLineConfig(qLineConfig);
            if (set != null) {
                exploration.addSet(set);
            }
        }
    }

    /**
     * Loads Explorables (PointOfInterest and LootChest) from the explorables YAML file.
     */
    private void loadExplorables() {
        if (!explorablesFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(explorablesFile);

        // Load Points of Interest
        List<String> poiConfigs = config.getStringList("pointsOfInterest");
        for (String poiConfigString : poiConfigs) {
            QLineConfig qLineConfig = new QLineConfig(poiConfigString);
            PointOfInterest poi = PointOfInterest.fromQLineConfig(qLineConfig);
            if (poi != null) {
                exploration.addPointOfInterest(poi);

                // Add to the appropriate set if it exists
                String parentSetId = qLineConfig.getString("parentSet");
                if (parentSetId != null && !parentSetId.isEmpty()) {
                    ExplorationSet set = exploration.getSet(parentSetId);
                    if (set != null) {
                        poi.setSet(set);
                        set.addExplorable(poi);
                    }
                }
            }
        }

        // Load Loot Chests
        List<String> chestConfigs = config.getStringList("lootChests");
        for (String chestConfigString : chestConfigs) {
            QLineConfig qLineConfig = new QLineConfig(chestConfigString);
            LootChest chest = LootChest.fromQLineConfig(qLineConfig);
            if (chest != null) {
                exploration.addLootChest(chest);

                // Add to the appropriate set if it exists
                String parentSetId = qLineConfig.getString("parentSet");
                if (parentSetId != null && !parentSetId.isEmpty()) {
                    ExplorationSet set = exploration.getSet(parentSetId);
                    if (set != null) {
                        chest.setSet(set);
                        set.addExplorable(chest);
                    }
                }
            }
        }
    }

    /**
     * Establishes parent-child relationships between ExplorationSets after all sets are loaded.
     * This is done in a separate pass to avoid circular dependency issues during loading.
     */
    private void establishSetHierarchy() {
        if (!explorationSetsFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(explorationSetsFile);
        List<String> setConfigs = config.getStringList("sets");

        for (String setConfigString : setConfigs) {
            QLineConfig qLineConfig = new QLineConfig(setConfigString);
            String setId = qLineConfig.getName();
            ExplorationSet set = exploration.getSet(setId);

            if (set != null) {
                // Set parent relationship
                String parentId = qLineConfig.getString("parentSet");
                if (parentId != null && !parentId.isEmpty()) {
                    ExplorationSet parent = exploration.getSet(parentId);
                    if (parent != null) {
                        set.setParent(parent);
                    }
                }
            }
        }
    }

    /**
     * Saves all exploration data to YAML files.
     */
    public void save() {
        saveExplorationSets();
        saveExplorables();
    }

    /**
     * Saves ExplorationSets to the sets YAML file as a list of QLineConfig strings.
     */
    private void saveExplorationSets() {
        YamlConfiguration config = new YamlConfiguration();
        List<String> setConfigs = new ArrayList<>();

        for (ExplorationSet set : exploration.getSets()) {
            QLineConfig qLineConfig = set.toQLineConfig();
            setConfigs.add(qLineConfig.toString());
        }

        config.set("sets", setConfigs);

        try {
            config.save(explorationSetsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves Explorables to the explorables YAML file as lists of QLineConfig strings.
     */
    private void saveExplorables() {
        YamlConfiguration config = new YamlConfiguration();

        // Save Points of Interest
        List<String> poiConfigs = new ArrayList<>();
        for (PointOfInterest poi : exploration.getPointsOfInterest()) {
            QLineConfig qLineConfig = poi.toQLineConfig();
            poiConfigs.add(qLineConfig.toString());
        }
        config.set("pointsOfInterest", poiConfigs);

        // Save Loot Chests
        List<String> chestConfigs = new ArrayList<>();
        for (LootChest chest : exploration.getLootChests()) {
            QLineConfig qLineConfig = chest.toQLineConfig();
            chestConfigs.add(qLineConfig.toString());
        }
        config.set("lootChests", chestConfigs);

        try {
            config.save(explorablesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a new ExplorationSet and saves immediately.
     */
    public void addExplorationSet(ExplorationSet set) {
        if (set != null) {
            exploration.addSet(set);
            saveExplorationSets();
        }
    }

    /**
     * Removes an ExplorationSet and saves immediately.
     */
    public void removeExplorationSet(ExplorationSet set) {
        if (set != null) {
            exploration.removeSet(set);
            saveExplorationSets();
        }
    }

    /**
     * Adds a new PointOfInterest and saves immediately.
     */
    public void addPointOfInterest(PointOfInterest poi) {
        if (poi != null) {
            exploration.addPointOfInterest(poi);
            saveExplorables();
        }
    }

    /**
     * Removes a PointOfInterest and saves immediately.
     */
    public void removePointOfInterest(PointOfInterest poi) {
        if (poi != null) {
            exploration.removePointOfInterest(poi);
            saveExplorables();
        }
    }

    /**
     * Adds a new LootChest and saves immediately.
     */
    public void addLootChest(LootChest chest) {
        if (chest != null) {
            exploration.addLootChest(chest);
            saveExplorables();
        }
    }

    /**
     * Removes a LootChest and saves immediately.
     */
    public void removeLootChest(LootChest chest) {
        if (chest != null) {
            exploration.removeLootChest(chest);
            saveExplorables();
        }
    }
}
