package de.erethon.questsxl.quest;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.action.ActionManager;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.condition.ConditionManager;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.players.QPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QQuest {

    File file;
    YamlConfiguration cfg;

    String name;
    String displayName;
    String description;
    private final List<QStage> stages = new ArrayList<>();
    private final Set<QCondition> conditions = new HashSet<>();
    private final Set<QAction> startActions = new HashSet<>();
    private final Set<QAction> rewards = new HashSet<>();


    public QQuest(File file) {
        String fileName = file.getName();
        name = fileName.replace(".yml", "");
        cfg =  YamlConfiguration.loadConfiguration(file);
        load();
    }

    public void reward(QPlayer player) {
        for (QAction action : rewards) {
            action.play(player.getPlayer());
        }
    }

    public boolean canStartQuest(QPlayer qPlayer) {
        for (QCondition condition : conditions) {
            if (!condition.check(qPlayer)) {
                qPlayer.send(condition.getDisplayText());
                return false;
            }
        }
        return true;
    }

    public List<QStage> getStages() {
        return stages;
    }

    public String getName() {
        return name;
    }

    public void setName(String nn) {
        name = nn;
    }

    public void load() {
        // General
        displayName = cfg.getString("displayName");
        description = cfg.getString("description");
        // Conditions
        ConfigurationSection conditionSection = cfg.getConfigurationSection("conditions");
        if (conditionSection != null) {
            conditions.addAll(ConditionManager.loadConditions(conditionSection));
        }
        // Start actions
        ConfigurationSection startActionSection = cfg.getConfigurationSection("onStart");
        if (startActionSection != null) {
            startActions.addAll(ActionManager.loadActions(startActionSection));
        }
        // Reward actions
        ConfigurationSection rewardActionSection = cfg.getConfigurationSection("onFinish");
        if (rewardActionSection != null) {
            rewards.addAll(ActionManager.loadActions(rewardActionSection));
        }
        // Stages
        ConfigurationSection stageSection = cfg.getConfigurationSection("stages");
        if (stageSection == null) {
            MessageUtil.log("Quest " + name + " does not contain any stages!");
            return;
        }
        for (String key : stageSection.getKeys(false)) {
            ConfigurationSection stageS = stageSection.getConfigurationSection(key);
            int id = Integer.parseInt(key);
            QStage stage = new QStage(this, id);
            stage.load(stageS);
            stages.add(stage);
        }
        MessageUtil.log("Loaded quest " + name + " with " + stages.size() + " stages.");
    }
}
