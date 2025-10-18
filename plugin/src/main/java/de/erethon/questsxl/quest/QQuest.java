package de.erethon.questsxl.quest;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.Completable;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.QStage;
import de.erethon.questsxl.common.QTranslatable;
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

public class QQuest implements Completable, QComponent {

    public YamlConfiguration cfg;
    String name;
    private QTranslatable displayName;
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
            QuestsXL.get().getErrors().add(new FriendlyError("Quest: " + this.getName(), "Datei ungültig.", "Datei " + file.getName() + " ist ungültig.", "Wahrscheinlich falsche Einrückung."));
            return;
        }
        try {
            load();
        } catch (Exception e) {
            QuestsXL.get().getErrors().add(new FriendlyError(fileName, "Failed to load quest", e.getMessage(), "Format invalid?"));
            e.printStackTrace();
        }
    }

    @Override
    public void reward(QPlayer player) {
        for (QAction action : rewards) {
            try {
                action.play(player);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError("Quest: " + this.getName(), "Failed to reward player", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addPlayer(player);
                error.addStacktrace(e.getStackTrace());
                QuestsXL.get().getErrors().add(error);
            }
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
            try {
                if (!condition.check(qPlayer)) {
                    qPlayer.send(condition.getDisplayText());
                    return false;
                }
            } catch (Exception e) {
                FriendlyError error = new FriendlyError("Quest: " + this.getName(), "Failed to check condition", e.getMessage(), "Condition: " + condition.getClass().getSimpleName());
                error.addPlayer(qPlayer);
                error.addStacktrace(e.getStackTrace());
                QuestsXL.get().getErrors().add(error);
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

    public QTranslatable displayName() {
        return displayName;
    }

    public String getDescription() {
        if (description == null) {
            return "No description set.";
        }
        return description;
    }
    @Override
    public QComponent getParent() {
        return null;
    }

    @Override
    public void setParent(QComponent parent) {
        // We are the top-level component, so we don't need to set a parent.
    }

    @Override
    public String id() {
        return name;
    }


    @Override
    public void load() {
        // General
        if (cfg.contains("displayName")) {
            displayName = QTranslatable.fromString(cfg.getString("displayName"));
        }
        description = cfg.getString("description", "<no description>");
        // Conditions
        if (cfg.contains("conditions")) {
            conditions.addAll((Collection<? extends QCondition>) QConfigLoader.load(this, "conditions", cfg, QRegistries.CONDITIONS));
            for (QCondition condition : conditions) {
                condition.setParent(this);
            }
        }
        // Start actions
        if (cfg.contains("onStart")) {
            startActions.addAll((Collection<? extends QAction>) QConfigLoader.load(this, "onStart", cfg, QRegistries.ACTIONS));
            for (QAction action : startActions) {
                action.setParent(this);
            }
        }
        // Reward actions
        if (cfg.contains("onFinish")) {
            rewards.addAll((Collection<? extends QAction>) QConfigLoader.load(this, "onFinish", cfg, QRegistries.ACTIONS));
            for (QAction action : rewards) {
                action.setParent(this);
            }
        }
        // Stages
        ConfigurationSection stageSection = cfg.getConfigurationSection("stages");
        if (stageSection == null) {
            QuestsXL.log("Quest " + name + " does not contain any stages!");
            return;
        }
        for (String key : stageSection.getKeys(false)) {
            ConfigurationSection stageS = stageSection.getConfigurationSection(key);
            if (stageS == null) {
                QuestsXL.get().getErrors().add(new FriendlyError("Quest: " + this.getName(), "Stage '" + key + "' konnte nicht geladen werden", "stage section is null", "Wahrscheinlich falsche Einrückung."));
                continue;
            }
            int id = Integer.parseInt(key);
            QStage stage = new QStage(this, id);
            try {
                stage.load(this, stageS);
            } catch (Exception e) {
                QuestsXL.get().getErrors().add(new FriendlyError("Quest: " + this.getName(), "Stage " + id + " konnte nicht geladen werden.", e.getMessage(), "..."));
                e.printStackTrace();
            }
            stages.add(stage);
        }
        QuestsXL.log("Loaded quest " + name + " with " + stages.size() + " stages.");
    }
}
