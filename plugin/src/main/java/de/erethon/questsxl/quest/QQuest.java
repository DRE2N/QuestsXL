package de.erethon.questsxl.quest;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.Completable;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.QStage;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QQuest implements Completable {

    File file;
    YamlConfiguration cfg;
    String name;
    String displayName;
    String description;
    private final List<QStage> stages = new ArrayList<>();
    private final Set<QCondition> conditions = new HashSet<>();
    private final Set<QAction> startActions = new HashSet<>();
    private final Set<QAction> rewards = new HashSet<>();

    /** A quest is a collection of stages that are completed in order to progress. Quests can only belong to {@link QPlayer}s.
     * @param file the file that contains the quest data.
     */
    public QQuest(File file) {
        String fileName = file.getName();
        name = fileName.replace(".yml", "");
        cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getKeys(false).size() == 0) {
            QuestsXL.getInstance().getErrors().add(new FriendlyError("Quest: " + this.getName(), "Datei ungültig.", "Datei " + file.getName() + " ist ungültig.", "Wahrscheinlich falsche Einrückung."));
            return;
        }
        try {
            load();
        } catch (Exception e) {
            QuestsXL.getInstance().getErrors().add(new FriendlyError(fileName, "Failed to load quest", e.getMessage(), "Format invalid?"));
            e.printStackTrace();
        }
    }

    @Override
    public void reward(QPlayer player) {
        for (QAction action : rewards) {
            action.play(player);
        }
    }

    @Override
    public void reward(Set<QPlayer> players) {
        for (QPlayer player : players) {
            reward(player);
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

    @Override
    public List<QStage> getStages() {
        return stages;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String nn) {
        name = nn;
    }

    public String getDisplayName() {
        if (displayName == null) {
            return name;
        }
        return displayName;
    }

    public String getDescription() {
        if (description == null) {
            return "No description set.";
        }
        return description;
    }

    @Override
    public void load() {
        // General
        displayName = cfg.getString("displayName", "<no display name>");
        description = cfg.getString("description", "<no description>");
        // Conditions
        if (cfg.contains("conditions")) {
            conditions.addAll((Collection<? extends QCondition>) QConfigLoader.load("conditions", cfg, QRegistries.CONDITIONS));
        }
        // Start actions
        if (cfg.contains("onStart")) {
            startActions.addAll((Collection<? extends QAction>) QConfigLoader.load("onStart", cfg, QRegistries.ACTIONS));
        }
        // Reward actions
        if (cfg.contains("onFinish")) {
            rewards.addAll((Collection<? extends QAction>) QConfigLoader.load("onFinish", cfg, QRegistries.ACTIONS));
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
            try {
                stage.load(stageS);
            } catch (Exception e) {
                QuestsXL.getInstance().getErrors().add(new FriendlyError("Quest: " + this.getName(), "Stage " + id + " konnte nicht geladen werden.", e.getMessage(), "..."));
                e.printStackTrace();
            }
            stages.add(stage);
        }
        MessageUtil.log("Loaded quest " + name + " with " + stages.size() + " stages.");
    }
}
