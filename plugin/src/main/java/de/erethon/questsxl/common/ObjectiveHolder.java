package de.erethon.questsxl.common;

import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QObjective;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface ObjectiveHolder {
    void addObjective(@NotNull ActiveObjective objective);
    boolean hasObjective(@NotNull QObjective objective);
    Set<ActiveObjective> getCurrentObjectives();
    void removeObjective(@NotNull ActiveObjective objective);
    void clearObjectives();
    void progress(@NotNull Completable completable);
    Location getLocation();
    String getName();
    default void loadProgress(FileConfiguration cfg) {
        ConfigurationSection section = cfg.getConfigurationSection("objectives");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection objSection = section.getConfigurationSection(key);
                if (objSection != null) {
                    ActiveObjective objective = ActiveObjective.load(this, objSection);
                    getCurrentObjectives().add(objective);
                }
            }
        }
    }
    default void saveProgress(FileConfiguration cfg) {
        ConfigurationSection section = cfg.createSection("objectives");
        int index = 0;
        for (ActiveObjective objective : getCurrentObjectives()) {
            ConfigurationSection objSection = section.createSection(String.valueOf(index));
            objective.save(objSection);
            index++;
        }
    }
}
