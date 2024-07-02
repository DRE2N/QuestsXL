package de.erethon.questsxl.global;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.objective.QObjective;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GlobalObjectives {

    List<QObjective> objectives = new ArrayList<>();

    public List<QObjective> getObjectives() {
        return objectives;
    }

    public GlobalObjectives(File file) throws InvalidConfigurationException {
        load(file);
    }

    public void load(File file) throws InvalidConfigurationException {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String s : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(s);
            if (section == null) {
                continue;
            }
            objectives.addAll((Collection<? extends QObjective>) QConfigLoader.load("objectives", section, QRegistries.OBJECTIVES));
        }
        for (QObjective objective : objectives) {
            objective.setGlobal(true);
        }
        MessageUtil.log("Loaded " + objectives.size() + " global objectives.");

    }
}
