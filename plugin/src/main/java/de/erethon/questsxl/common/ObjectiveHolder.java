package de.erethon.questsxl.common;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QObjective;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Represents an object that can hold objectives.
 */
public interface ObjectiveHolder {
    void addObjective(@NotNull ActiveObjective objective);
    boolean hasObjective(@NotNull QObjective objective);
    Set<ActiveObjective> getCurrentObjectives();
    void removeObjective(@NotNull ActiveObjective objective);
    void clearObjectives();
    void progress(@NotNull Completable completable);
    Location getLocation();
    String getName();
    String getUniqueId();
    default void loadProgress(FileConfiguration cfg) {
        ConfigurationSection section = cfg.getConfigurationSection("objectives");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection objSection = section.getConfigurationSection(key);
                if (objSection != null) {
                    try {
                        ActiveObjective objective = ActiveObjective.load(this, objSection);
                        getCurrentObjectives().add(objective);
                    } catch (Exception e) {
                        MessageUtil.log("Failed to load objective from config: " + key + " in " + cfg.getName());
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    default void saveProgress(FileConfiguration cfg) {
        ConfigurationSection section = cfg.createSection("objectives");
        int index = 0;
        for (ActiveObjective objective : getCurrentObjectives()) {
            ConfigurationSection objSection = section.createSection(String.valueOf(index));
            try {
                objective.save(objSection);
                index++;
            } catch (Exception e) {
                MessageUtil.log("Failed to save objective to config: " + objective.getObjective().getClass().getSimpleName() + " in " + cfg.getName());
                e.printStackTrace();
            }
        }
    }

    default ActiveObjective findObjective(QObjective objective) {
        for (ActiveObjective activeObjective : getCurrentObjectives()) {
            if (activeObjective.getObjective().equals(objective)) {
                return activeObjective;
            }
        }
        return null;
    }
}
