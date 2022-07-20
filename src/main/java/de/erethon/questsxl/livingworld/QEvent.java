package de.erethon.questsxl.livingworld;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.config.storage.Nullability;
import de.erethon.bedrock.config.storage.StorageData;
import de.erethon.bedrock.config.storage.StorageDataContainer;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.Action;
import de.erethon.questsxl.action.ActionManager;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.condition.ConditionManager;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.Completable;
import de.erethon.questsxl.quest.QStage;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class QEvent extends StorageDataContainer implements Completable {

    File file;
    YamlConfiguration cfg;

    String id;
    List<QStage> stages = new ArrayList<>();
    List<QAction> updateActions = new ArrayList<>();
    Set<QCondition> startConditions = new HashSet<>();
    @StorageData(type = Integer.class, path = "cooldown")
    int cooldown;

    // State - Persistence
    // @StorageData(type = Enum.class, path = "state.state") - NYI
    EventState state;
    @StorageData(type = HashMap.class, keyTypes = String.class, valueTypes = Integer.class, nullability = Nullability.IGNORE)
    private final Map<String, Integer> scores = new HashMap<>();

    @StorageData(type = Location.class, path = "state.center", nullability = Nullability.FORBID)
    private Location centerLocation = null;

    @StorageData(type = Long.class, path = "state.timeLastCompleted")
    long timeLastCompleted;

    QStage currentStage;

    // State - runtime stuff
    private int playersInRange = 0;

    public QEvent(File file) {
        super(file, 1);
        String fileName = file.getName();
        this.file = file;
        id = fileName.replace(".yml", "");
        cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getKeys(false).size() == 0) {
            QuestsXL.getInstance().getErrors().add(new FriendlyError("Event: " + this.getName(), "Datei ungültig.", "Datei " + file.getName() + " ist ungültig.", "Wahrscheinlich falsche Einrückung."));
            return;
        }
        load();
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public void reward(QPlayer player) {

    }

    @Override
    public void reward(Set<QPlayer> players) {

    }

    @Override
    public List<QStage> getStages() {
        return stages;
    }

    public String getId() {
        return id;
    }

    public void complete() {
        state = EventState.COMPLETED;
        timeLastCompleted = System.currentTimeMillis();
    }

    public void update() {
        switch (state) {
            case ACTIVE -> {
                playersInRange = centerLocation.getNearbyEntitiesByType(Player.class, 20).size();
                for (QAction action : updateActions) {
                    action.play(this);
                }
            }
            case NOT_STARTED -> {
                for (QCondition condition : startConditions) {
                    if (!condition.check(this)) {
                        return;
                    }
                    stages.get(0).start(this);
                    MessageUtil.log("Event " + getName() + " started.");
                    state = EventState.ACTIVE;
                }
            }
            case COMPLETED -> {
                if (System.currentTimeMillis() - timeLastCompleted > cooldown * 1000L) {
                    state = EventState.NOT_STARTED;
                }
            }
            case DISABLED -> {
                // Nothing to see here.
            }
        }
    }

    public void progress() {
        MessageUtil.log("Progressing event " + id);
        QStage next = null;
        int currentID = currentStage.getId();
        for (QStage stage : getStages()) {
            if (stage.getId() == currentID + 1) {
                next = stage;
            }
        }
        if (next == null) {
            finish();
            return;
        }
        currentStage = next;
    }

    public void finish() {

    }

    public void addScore(@NotNull String score, int amount) {
        setScore(score, scores.getOrDefault(score, 0) + amount);
    }

    public void removeScore(@NotNull String score, int amount) {
        setScore(score, scores.getOrDefault(score, 0) - amount);
    }

    public void setScore(@NotNull String score, int amount) {
        scores.put(score, amount);
    }

    public int getScore(@NotNull String id) {
        return scores.getOrDefault(id, 0);
    }

    public EventState getState() {
        return state;
    }

    public int getPlayersInRange() {
        return playersInRange;
    }

    public Location getCenterLocation() {
        return centerLocation;
    }

    public void setCenterLocation(Location centerLocation) {
        this.centerLocation = centerLocation;
    }

    @Override
    public void load() {
        ConfigurationSection conditionSection = cfg.getConfigurationSection("startConditions");
        if (conditionSection != null) {
            ConditionManager.loadConditions(id, conditionSection);
        }

        ConfigurationSection updateSection = cfg.getConfigurationSection("onUpdate");
        if (updateSection != null) {
            ActionManager.loadActions(id, updateSection);
        }

        ConfigurationSection stageSection = cfg.getConfigurationSection("stages");
        if (stageSection == null) {
            MessageUtil.log("Event " + id + " does not contain any stages!");
            return;
        }
        for (String key : stageSection.getKeys(false)) {
            ConfigurationSection stageS = stageSection.getConfigurationSection(key);
            int id = Integer.parseInt(key);
            QStage stage = new QStage(this, id);
            try {
                stage.load(stageS);
            } catch (Exception e) {
                QuestsXL.getInstance().getErrors().add(new FriendlyError("Event: " + this.getName(), "Stage " + id + " konnte nicht geladen werden.", e.getMessage(), "..."));
            }
            stages.add(stage);
        }

        super.load();
        MessageUtil.log("Loaded event " + id + " with " + stages.size() + " stages.");
    }

    public void save() {
        super.saveData();
    }

}
