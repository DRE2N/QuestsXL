package de.erethon.questsxl.livingworld;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.config.storage.Nullability;
import de.erethon.bedrock.config.storage.StorageData;
import de.erethon.bedrock.config.storage.StorageDataContainer;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.ActionManager;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.condition.ConditionManager;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import de.erethon.questsxl.quest.Completable;
import de.erethon.questsxl.quest.QStage;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class QEvent extends StorageDataContainer implements Completable {

    QPlayerCache playerCache = QuestsXL.getInstance().getPlayerCache();

    File file;
    YamlConfiguration cfg;

    String id;
    List<QStage> stages = new ArrayList<>();
    List<QAction> updateActions = new ArrayList<>();
    Set<QCondition> startConditions = new HashSet<>();

    Map<Integer, Set<QAction>> rewards = new HashMap<>();

    @StorageData(type = Integer.class, path = "cooldown")
    int cooldown;

    @StorageData(type = Integer.class, path = "range")
    int range;

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
    private Set<QPlayer> playersInRange = new HashSet<>();
    private final Map<QPlayer, Integer> eventParticipation = new HashMap<>();

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
        reward();
    }

    @Override
    public void reward(Set<QPlayer> players) {
        reward();
    }

    public void reward() {
        for (Map.Entry<QPlayer, Integer> playerEntry : eventParticipation.entrySet()) {
            for (Map.Entry<Integer, Set<QAction>> reward : rewards.entrySet()) {
                if (reward.getKey() >= playerEntry.getValue()) {
                    for (QAction action : reward.getValue()) {
                        action.play(playerEntry.getKey());
                    }
                }
            }
        }
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
                playersInRange.clear();
                for (Player player : centerLocation.getNearbyEntitiesByType(Player.class, range)) {
                    playersInRange.add(playerCache.getByPlayer(player));
                }
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

    public void setCurrentStage(int id) {
        currentStage = stages.get(id);
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

    public Set<QPlayer> getPlayersInRange() {
        return playersInRange;
    }

    public Location getCenterLocation() {
        return centerLocation;
    }

    public void setCenterLocation(Location centerLocation) {
        this.centerLocation = centerLocation;
    }


    public void participate(@NotNull QPlayer player, int amount) {
        eventParticipation.put(player, amount);
    }

    public int getEventParticipation(@NotNull QPlayer player) {
        return eventParticipation.get(player);
    }

    public Map<QPlayer, Integer> getParticipants() {
        return eventParticipation;
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

        ConfigurationSection rewardSection = cfg.getConfigurationSection("rewards");
        if (rewardSection == null) {
            MessageUtil.log("Event " + id + " does not contain any rewards!");
            return;
        }
        for (String key : rewardSection.getKeys(false)) {
            ConfigurationSection rewardEntry = rewardSection.getConfigurationSection(key);
            int id = Integer.parseInt(key);
            if (rewardEntry != null) {
                rewards.put(id, ActionManager.loadActions(rewardEntry.getName(), rewardEntry));
            }
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
