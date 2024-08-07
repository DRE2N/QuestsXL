package de.erethon.questsxl.livingworld;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QLoadable;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.Scorable;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import de.erethon.questsxl.common.Completable;
import de.erethon.questsxl.common.QStage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class QEvent implements Completable, ObjectiveHolder, Scorable {

    QPlayerCache playerCache = QuestsXL.getInstance().getPlayerCache();

    File file;
    YamlConfiguration cfg;

    String id;
    List<QStage> stages = new ArrayList<>();
    List<QAction> updateActions = new ArrayList<>();
    Set<QCondition> startConditions = new HashSet<>();

    Map<Integer, Set<QAction>> rewards = new HashMap<>();

    Set<ActiveObjective> currentObjectives = new HashSet<>();

    private String objectiveDisplayText;

    int cooldown;

    int range;

    EventState state;

    private final Map<String, Integer> scores = new HashMap<>();

    private Location centerLocation = null;

    long timeLastCompleted;

    QStage currentStage;

    // State - runtime stuff
    private Set<QPlayer> playersInRange = new HashSet<>();
    private final Map<QPlayer, Integer> eventParticipation = new HashMap<>();

    public QEvent(File file) {
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
                if (reward.getKey() <= playerEntry.getValue()) {
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
                for (Player player : centerLocation.getNearbyPlayers(range)) {
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
                }
                stages.get(0).start(this);
                currentStage = stages.get(0);
                MessageUtil.log("Event " + getName() + " started with stage " + currentStage.getId() + " with " + getCurrentObjectives().size() + " objectives.");
                state = EventState.ACTIVE;
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

    public void startFromAction(boolean skipConditions) {
        if (state == EventState.DISABLED || state == EventState.ACTIVE) {
            return;
        }
        for (QCondition condition : startConditions) {
            if (!condition.check(this) && !skipConditions) {
                return;
            }
        }
        stages.get(0).start(this);
        currentStage = stages.get(0);
        MessageUtil.log("Event " + getName() + " started from action.");
        state = EventState.ACTIVE;
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
        MessageUtil.log("Event " + getName() + " finished.");
        reward();
        state = EventState.COMPLETED;
        clearObjectives();
    }

    public void setCurrentStage(int id) {
        currentStage = stages.get(id);
    }

    @Override
    public void addScore(@NotNull String score, int amount) {
        setScore(score, scores.getOrDefault(score, 0) + amount);
    }

    @Override
    public void removeScore(@NotNull String score, int amount) {
        setScore(score, scores.getOrDefault(score, 0) - amount);
    }

    @Override
    public void setScore(@NotNull String score, int amount) {
        scores.put(score, amount);
    }

    @Override
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
        ConfigurationSection locationSection = cfg.getConfigurationSection("startLocation");
        cooldown = cfg.getInt("cooldown", 0);
        range = cfg.getInt("range", 32);
        MessageUtil.log(locationSection.getKeys(false).toString());
        MessageUtil.log("World: " + locationSection.getString("world"));
        Bukkit.getWorlds().forEach(world -> MessageUtil.log(world.getName()));
        String worldName = locationSection.getString("world", "Erethon");
        double x = locationSection.getDouble("x");
        double y = locationSection.getDouble("y");
        double z = locationSection.getDouble("z");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            MessageUtil.log("The condition " + locationSection.getName() + " contains a location for a world that is not loaded: " + worldName);
            return;
        }
        centerLocation = new Location(world, x, y, z);
        if (cfg.contains("startConditions")) {
            startConditions.addAll((Collection<? extends QCondition>) QConfigLoader.load("startConditions", cfg, QRegistries.CONDITIONS));
        }

        state = EventState.valueOf(cfg.getString("state.state", "NOT_STARTED"));

        if (cfg.contains("onUpdate")) {
            updateActions.addAll((Collection<? extends QAction>) QConfigLoader.load("onUpdate", cfg, QRegistries.ACTIONS));
        }

        ConfigurationSection rewardSection = cfg.getConfigurationSection("rewards");
        if (rewardSection == null) {
            MessageUtil.log("Event " + id + " does not contain any rewards!");
            return;
        }
        for (String key : rewardSection.getKeys(false)) {
            ConfigurationSection rewardEntry = rewardSection.getConfigurationSection(key);
            int id = Integer.parseInt(key);
            Set<QLoadable> rewardSet = (Set<QLoadable>) QConfigLoader.load("rewards", rewardEntry, QRegistries.ACTIONS);
            if (rewardSet == null) {
                continue;
            }
            Set<QAction> actionSet = new HashSet<>();
            for (QLoadable loadable : rewardSet) {
                if (loadable instanceof QAction action) {
                    actionSet.add(action);
                }
            }
            rewards.put(id, actionSet);
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
                QuestsXL.getInstance().getErrors().add(new FriendlyError("Event: " + this.getName(), "Stage " + id + " konnte nicht geladen werden.", e.getMessage(), "...").addStacktrace(e.getStackTrace()));
            }
            stages.add(stage);
        }

        MessageUtil.log("Loaded event " + id + " with " + stages.size() + " at " + centerLocation.getWorld().getName() + " / " + centerLocation.getX() + " / " + centerLocation.getY() + " / " + centerLocation.getZ());
    }

    public void save() {

    }

    @Override
    public void addObjective(@NotNull ActiveObjective objective) {
        currentObjectives.add(objective);
    }

    @Override
    public boolean hasObjective(@NotNull QObjective objective) {
        return currentObjectives.stream().anyMatch(o -> o.getObjective() == objective);
    }

    @Override
    public Set<ActiveObjective> getCurrentObjectives() {
        return currentObjectives;
    }

    @Override
    public void removeObjective(@NotNull ActiveObjective objective) {
        currentObjectives.remove(objective);
    }

    @Override
    public void clearObjectives() {
        currentObjectives.clear();
    }

    @Override
    public void progress(@NotNull Completable completable) {
        progress();
    }

    @Override
    public Location getLocation() {
        return centerLocation;
    }

    public String getObjectiveDisplayText() {
        return objectiveDisplayText;
    }

    public void setObjectiveDisplayText(String objectiveDisplayText) {
        this.objectiveDisplayText = objectiveDisplayText;
    }
}
