package de.erethon.questsxl.global;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.Completable;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.QStage;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class GlobalObjectives implements Completable, QComponent {

    List<QObjective> objectives = new ArrayList<>();

    public List<QObjective> getObjectives() {
        return objectives;
    }
    private QStage stage;

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
            objectives.addAll((Collection<? extends QObjective>) QConfigLoader.load(this, "objectives", section, QRegistries.OBJECTIVES));
            for (QObjective objective : objectives) {
                objective.setParent(this);
            }
        }
        for (QObjective objective : objectives) {
            objective.setGlobal(true);
        }
        MessageUtil.log("Loaded " + objectives.size() + " global objectives.");

    }

    // Dummy methods, so we can simply implement the Completable interface
    @Override
    public void load() {

    }

    @Override
    public String getName() {
        return "global";
    }

    @Override
    public void reward(QPlayer player) {

    }

    @Override
    public void reward(Set<QPlayer> players) {

    }

    @Override
    public List<QStage> getStages() {
        return List.of(stage);
    }

    @Override
    public QComponent getParent() {
        return null;
    }

    @Override
    public void setParent(QComponent parent) {
        // We are the top-level component, so we don't need to set a parent.
    }
}
