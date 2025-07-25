package de.erethon.questsxl.livingworld;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.common.Quester;
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
import java.io.IOException;
import java.util.*;

public class QEvent implements Completable, ObjectiveHolder, Scorable, QComponent, Quester, Explorable {

    private final QPlayerCache playerCache = QuestsXL.getInstance().getPlayerCache();
    private final QuestsXL plugin = QuestsXL.getInstance();

    private final File file;
    private final YamlConfiguration cfg;
    private boolean isValid;
    private QComponent parent;

    private final String id;
    private final List<QStage> stages = new ArrayList<>();
    private final List<QAction> updateActions = new ArrayList<>();
    private final List<QAction> startActions = new ArrayList<>();
    private final Set<QCondition> startConditions = new HashSet<>();

    private final Map<Integer, Set<QAction>> rewards = new HashMap<>();
    private boolean giveAllRewards = true;

    private final Set<ActiveObjective> currentObjectives = new HashSet<>();

    private String objectiveDisplayText;

    private int cooldown;

    private int range;
    private int canActivateRange;

    private EventState state;

    private final Map<String, Integer> scores = new HashMap<>();

    private Location centerLocation = null;

    private long timeLastCompleted;

    private QStage currentStage;

    // State - runtime stuff
    private final Set<QPlayer> playersInRange = new HashSet<>();
    private final Map<QPlayer, Integer> eventParticipation = new HashMap<>();

    public QEvent(File file) {
        String fileName = file.getName();
        this.file = file;
        id = fileName.replace(".yml", "");
        cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getKeys(false).isEmpty()) {
            QuestsXL.getInstance().getErrors().add(new FriendlyError("Event: " + this.getName(), "Datei ungültig.", "Datei " + file.getName() + " ist ungültig.", "Wahrscheinlich falsche Einrückung."));
            isValid = false;
            return;
        }
        load();
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public String getUniqueId() {
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
            if (giveAllRewards) {
                for (Map.Entry<Integer, Set<QAction>> reward : rewards.entrySet()) {
                    if (reward.getKey() <= playerEntry.getValue()) {
                        for (QAction action : reward.getValue()) {
                            runRewardAction(playerEntry, action);
                        }
                    }
                }
                return;
            }
            int highestRewardKey = rewards.keySet().stream()
                    .filter(key -> key <= playerEntry.getValue())
                    .max(Integer::compareTo)
                    .orElse(-1);
            if (highestRewardKey != -1) {
                for (QAction action : rewards.get(highestRewardKey)) {
                    runRewardAction(playerEntry, action);
                }
            }
        }
    }

    private void runRewardAction(Map.Entry<QPlayer, Integer> playerEntry, QAction action) {
        try {
            action.play(playerEntry.getKey());
        } catch (Exception e) {
            FriendlyError error = new FriendlyError("Event: " + id, "Failed to execute reward action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
            error.addPlayer(playerEntry.getKey());
            error.addStacktrace(e.getStackTrace());
            QuestsXL.getInstance().getErrors().add(error);
        }
    }

    @Override
    public List<QStage> getStages() {
        return stages;
    }

    public String getId() {
        return id;
    }

    public void setState(EventState state) {
        this.state = state;
    }

    public void update() {
        long currentTime = System.currentTimeMillis();
        switch (state) {
            case ACTIVE -> {
                playersInRange.clear();
                for (Player player : centerLocation.getNearbyPlayers(range)) {
                    if (!Bukkit.getOnlinePlayers().contains(player)) {
                        continue;
                    }
                    playersInRange.add(playerCache.getByPlayer(player));
                }
                for (QAction action : updateActions) {
                    action.play(this);
                }
            }
            case NOT_STARTED -> {
                if (getCenterLocation().getNearbyPlayers(canActivateRange).isEmpty()) {
                    return; // No players in range
                }
                for (QCondition condition : startConditions) {
                    if (!condition.check(this)) {
                        return; // Conditions aren't met
                    }
                }
                if (timeLastCompleted > 0 && currentTime - timeLastCompleted < cooldown * 1000L) {
                    return; // Cooldown not over yet
                }
                startEvent();
                MessageUtil.log("Event " + getName() + " started with stage " + currentStage.getId() + " with " + getCurrentObjectives().size() + " objectives.");
            }
            case COMPLETED -> {
                if (currentTime - timeLastCompleted > cooldown * 1000L) {
                    state = EventState.NOT_STARTED;
                }
            }
            case DISABLED -> {
                // Nothing to see here.
            }
        }
    }

    public void startFromAction(boolean skipConditions) {
        if (state == EventState.DISABLED || state == EventState.ACTIVE && !skipConditions) {
            return;
        }
        for (QCondition condition : startConditions) {
            if (!condition.check(this) && !skipConditions) {
                return;
            }
        }
        startEvent();
        MessageUtil.log("Event " + getName() + " started from action.");
    }

    private void startEvent() {
        if (state == EventState.ACTIVE) {
            return;
        }
        stages.get(0).start(this);
        currentStage = stages.get(0);
        state = EventState.ACTIVE;
        for (QAction action : startActions) {
            action.play(this);
        }
    }

    public void progress() {
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
        clearObjectives();
        currentStage = next;
        next.start(this);
        MessageUtil.log("Event " + getName() + " progressed to stage " + currentStage.getId());
    }

    public void finish() {
        MessageUtil.log("Event " + getName() + " finished.");
        reward();
        state = EventState.COMPLETED;
        timeLastCompleted = System.currentTimeMillis();
        clearObjectives();
        playersInRange.clear();
        eventParticipation.clear();
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
        eventParticipation.put(player, eventParticipation.getOrDefault(player, 0) + amount);
        MessageUtil.log("Player " + player.getName() + " participated in event " + getName() + " with " + amount + " points. (Total: " + eventParticipation.get(player) + ")");
    }

    public int getEventParticipation(@NotNull QPlayer player) {
        if (!eventParticipation.containsKey(player)) {
            return 0;
        }
        return eventParticipation.get(player);
    }

    public Map<QPlayer, Integer> getParticipants() {
        return eventParticipation;
    }

    public void removePlayerOnDisconnect(QPlayer player) {
        playersInRange.remove(player); // Sometimes this seems necessary
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
    public void load() {
        ConfigurationSection locationSection = cfg.getConfigurationSection("startLocation");
        cooldown = cfg.getInt("cooldown", 0);
        range = cfg.getInt("range", 32);
        canActivateRange = cfg.getInt("canActivateRange", range);
        String worldName = locationSection.getString("world", "Erethon");
        double x = locationSection.getDouble("x");
        double y = locationSection.getDouble("y");
        double z = locationSection.getDouble("z");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new RuntimeException("The startLocation contains the world " + worldName + ", which does not exist.");
        }
        centerLocation = new Location(world, x, y, z);
        if (cfg.contains("startConditions")) {
            startConditions.addAll((Collection<? extends QCondition>) QConfigLoader.load(this, "startConditions", cfg, QRegistries.CONDITIONS));
            for (QCondition condition : startConditions) {
                condition.setParent(this);
            }
        }

        if (cfg.contains("onUpdate")) {
            updateActions.addAll((Collection<? extends QAction>) QConfigLoader.load(this, "onUpdate", cfg, QRegistries.ACTIONS));
            for (QAction action : updateActions) {
                action.setParent(this);
            }
        }

        if (cfg.contains("onStart")) {
            startActions.addAll((Collection<? extends QAction>) QConfigLoader.load(this, "onStart", cfg, QRegistries.ACTIONS));
            for (QAction action : startActions) {
                action.setParent(this);
            }
        }

        if (cfg.contains("rewards")) {
            ConfigurationSection rewardSection = cfg.getConfigurationSection("rewards");
            for (String key : rewardSection.getKeys(false)) {
                ConfigurationSection rewardEntry = rewardSection.getConfigurationSection(key);
                int id = Integer.parseInt(key);
                Set<QComponent> rewardSet = (Set<QComponent>) QConfigLoader.load(this, key, rewardSection, QRegistries.ACTIONS);
                if (rewardSet == null) {
                    continue;
                }
                Set<QAction> actionSet = new HashSet<>();
                for (QComponent loadable : rewardSet) {
                    if (loadable instanceof QAction action) {
                        actionSet.add(action);
                        action.setParent(this);
                    }
                }
                rewards.put(id, actionSet);
            }
        }
        giveAllRewards = cfg.getBoolean("giveAllRewards", true);

        ConfigurationSection stageSection = cfg.getConfigurationSection("stages");
        if (stageSection == null) {
            MessageUtil.log("Event " + id + " does not contain any stages!");
            return;
        }
        for (String key : stageSection.getKeys(false)) {
            ConfigurationSection stageS = stageSection.getConfigurationSection(key);
            if (stageS == null) {
                continue;
            }
            int id = Integer.parseInt(key);
            QStage stage = new QStage(this, id);
            stage.setParent(this);
            try {
                stage.load(this, stageS);
            } catch (Exception e) {
                QuestsXL.getInstance().getErrors().add(new FriendlyError("Event: " + this.getName(), "Stage " + id + " konnte nicht geladen werden.", e.getMessage(), "...").addStacktrace(e.getStackTrace()));
            }
            stages.add(stage);
        }

        // Restore state
        state = EventState.valueOf(cfg.getString("state.state", "NOT_STARTED"));
        int currentStageID = cfg.getInt("state.currentStage", 0);
        if (currentStageID != 0) {
            currentStage = stages.stream().filter(s -> s.getId() == currentStageID).findFirst().orElse(null);
        }
        timeLastCompleted = cfg.getLong("state.timeLastCompleted", 0);

        isValid = true;
        MessageUtil.log("Loaded event " + id + " with " + stages.size() + " stages at " + centerLocation.getWorld().getName() + " / " + centerLocation.getX() + " / " + centerLocation.getY() + " / " + centerLocation.getZ());
    }

    public void save() {
        cfg.set("state.state", state.name());
        if (currentStage != null) {
            cfg.set("state.currentStage", currentStage.getId());
        } else {
            cfg.set("state.currentStage", 0);
        }
        cfg.set("state.timeLastCompleted", (long) timeLastCompleted);
        saveProgress(cfg);
        try {
            cfg.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isValid() {
        return isValid;
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
        plugin.getObjectiveEventManager().unregister(objective);
        currentObjectives.remove(objective);
    }

    @Override
    public void clearObjectives() {
        for (ActiveObjective objective : currentObjectives) {
            plugin.getObjectiveEventManager().unregister(objective);
        }
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

    public int getRange() {
        return range;
    }

    public QStage getCurrentStage() {
        return currentStage;
    }

    public QPlayer getTopPlayer() {
        QPlayer top = null;
        int topScore = 0;
        for (Map.Entry<QPlayer, Integer> entry : eventParticipation.entrySet()) {
            if (entry.getValue() > topScore) {
                top = entry.getKey();
                topScore = entry.getValue();
            }
        }
        return top;
    }

    public String getObjectiveDisplayText() {
        return objectiveDisplayText;
    }

    public void setObjectiveDisplayText(String objectiveDisplayText) {
        this.objectiveDisplayText = objectiveDisplayText;
    }

    public YamlConfiguration getCfg() {
        return cfg;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public QTranslatable displayName() {
        return QTranslatable.fromString("TODO displayName"); // TODO: Implement translatable for events
    }

    @Override
    public Location location() {
        return getLocation();
    }
}
