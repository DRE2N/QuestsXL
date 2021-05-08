package de.erethon.questsxl.global;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.objectives.ObjectiveManager;
import de.erethon.questsxl.objectives.QObjective;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GlobalObjectives {

    List<QObjective> objectives = new ArrayList<>();

    public List<QObjective> getObjectives() {
        return objectives;
    }

    public GlobalObjectives(File file) {
        load(file);
    }

    public void load(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String s : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(s);
            if (section == null) {
                continue;
            }
            objectives.addAll(ObjectiveManager.loadObjectives(section));
        }
        MessageUtil.log("Loaded " + objectives.size() + " global objectives.");
    }
}
