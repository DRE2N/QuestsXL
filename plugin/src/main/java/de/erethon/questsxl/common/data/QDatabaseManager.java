package de.erethon.questsxl.common.data;

import de.erethon.bedrock.database.BedrockDBConnection;
import de.erethon.bedrock.database.EDatabaseManager;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.DatabaseManager;
import de.erethon.hecate.data.HPlayer;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.Completable;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.QStage;
import de.erethon.questsxl.global.GlobalObjectives;
import de.erethon.questsxl.livingworld.PlayerExplorer;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerDao;
import de.erethon.questsxl.player.QPlayerDao.ActiveQuestData;
import de.erethon.questsxl.player.QPlayerDao.CompletedQuestData;
import de.erethon.questsxl.player.QPlayerDao.ScoreData;
import de.erethon.questsxl.player.QPlayerDao.ObjectiveProgressData;
import de.erethon.questsxl.player.QPlayerDao.EventObjectiveProgressData;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

public class QDatabaseManager extends EDatabaseManager {

    private final Hecate hecate;
    private final DatabaseManager hecateDatabaseManager;

    private final Map<UUID, QPlayer> uuidqPlayerMap = new ConcurrentHashMap<>();
    private final Map<UUID, QPlayer> minecraftUUIDtoQPlayerMap = new ConcurrentHashMap<>();

    private final QPlayerDao playerDao;

    public QDatabaseManager(BedrockDBConnection connection) {
        super(connection, new ThreadPoolExecutor(2, 4, 60L, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>()));
        hecate = Bukkit.getPluginManager().getPlugin("Hecate") instanceof Hecate ? (Hecate) Bukkit.getPluginManager().getPlugin("Hecate") : null;
        hecateDatabaseManager = hecate != null ? hecate.getDatabaseManager() : null;
        if (hecateDatabaseManager == null) {
            throw new IllegalStateException("Hecate database manager is not initialized.");
        }
        playerDao = getDao(QPlayerDao.class);
    }

    @Override
    protected CompletableFuture<Void> initializeSchema() {
        return CompletableFuture.runAsync(() -> {
            try (var handle = jdbi.open()) {
                handle.execute("""
                    CREATE TABLE IF NOT EXISTS q_character_active_quests (
                        character_id UUID NOT NULL,
                        quest_id VARCHAR(255) NOT NULL,
                        current_stage INTEGER NOT NULL DEFAULT 0,
                        started_at BIGINT NOT NULL,
                        PRIMARY KEY (character_id, quest_id),
                        FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE
                    )
                """);

                // Create completed quests table
                handle.execute("""
                    CREATE TABLE IF NOT EXISTS q_character_completed_quests (
                        character_id UUID NOT NULL,
                        quest_id VARCHAR(255) NOT NULL,
                        completed_at BIGINT NOT NULL,
                        PRIMARY KEY (character_id, quest_id),
                        FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE
                    )
                """);

                // Create scores table
                handle.execute("""
                    CREATE TABLE IF NOT EXISTS q_character_scores (
                        character_id UUID NOT NULL,
                        score_name VARCHAR(255) NOT NULL,
                        score_value INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (character_id, score_name),
                        FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE
                    )
                """);

                // Create exploration data table
                handle.execute("""
                    CREATE TABLE IF NOT EXISTS q_character_exploration (
                        character_id UUID NOT NULL,
                        exploration_data TEXT NOT NULL,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (character_id),
                        FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE
                    )
                """);

                // Create objective progress table for characters
                handle.execute("""
                    CREATE TABLE IF NOT EXISTS q_character_objectives (
                        character_id UUID NOT NULL,
                        completable_type VARCHAR(50) NOT NULL,
                        completable_id VARCHAR(255) NOT NULL,
                        stage_id INTEGER NOT NULL,
                        objective_id VARCHAR(255) NOT NULL,
                        objective_type VARCHAR(255) NOT NULL,
                        progress INTEGER NOT NULL DEFAULT 0,
                        completed BOOLEAN NOT NULL DEFAULT FALSE,
                        objective_data TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (character_id, completable_type, completable_id, stage_id, objective_id),
                        FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE
                    )
                """);

                // Create objective progress table for events
                handle.execute("""
                    CREATE TABLE IF NOT EXISTS q_event_objectives (
                        event_id VARCHAR(255) NOT NULL,
                        stage_id INTEGER NOT NULL,
                        objective_id VARCHAR(255) NOT NULL,
                        objective_type VARCHAR(255) NOT NULL,
                        progress INTEGER NOT NULL DEFAULT 0,
                        completed BOOLEAN NOT NULL DEFAULT FALSE,
                        objective_data TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (event_id, stage_id, objective_id)
                    )
                """);
            } catch (Exception e) {
                QuestsXL.log("Failed to initialize schema: " + e.getMessage());
                e.printStackTrace();
            }
        }, asyncExecutor);
    }

    @Override
    protected void registerCustomMappers() {
        jdbi.registerRowMapper(ActiveQuestData.class, (rs, ctx) -> {
            return new ActiveQuestData(
                rs.getString("quest_id"),
                rs.getInt("current_stage"),
                rs.getLong("started_at")
            );
        });

        jdbi.registerRowMapper(CompletedQuestData.class, (rs, ctx) -> {
            return new CompletedQuestData(
                rs.getString("quest_id"),
                rs.getLong("completed_at")
            );
        });

        jdbi.registerRowMapper(ScoreData.class, (rs, ctx) -> {
            return new ScoreData(
                rs.getString("score_name"),
                rs.getInt("score_value")
            );
        });

        jdbi.registerRowMapper(ObjectiveProgressData.class, (rs, ctx) -> {
            return new ObjectiveProgressData(
                rs.getString("completable_type"),
                rs.getString("completable_id"),
                rs.getInt("stage_id"),
                rs.getString("objective_id"),
                rs.getString("objective_type"),
                rs.getInt("progress"),
                rs.getBoolean("completed"),
                rs.getString("objective_data")
            );
        });

        jdbi.registerRowMapper(EventObjectiveProgressData.class, (rs, ctx) -> {
            return new EventObjectiveProgressData(
                rs.getInt("stage_id"),
                rs.getString("objective_id"),
                rs.getString("objective_type"),
                rs.getInt("progress"),
                rs.getBoolean("completed"),
                rs.getString("objective_data")
            );
        });
    }

    public CompletableFuture<Void> saveObjectiveProgress(ActiveObjective activeObjective) {
        return CompletableFuture.runAsync(() -> {
            try {
                var holder = activeObjective.getHolder();
                var completable = activeObjective.getCompletable();
                var stage = activeObjective.getStage();
                var objective = activeObjective.getObjective();

                String objectiveId = generateObjectiveId(objective, stage);
                String objectiveType = QRegistries.OBJECTIVES.getId(objective.getClass());

                String objectiveData = serializeObjectiveData(activeObjective);

                if (holder instanceof QPlayer qPlayer) {
                    UUID characterId = getCurrentCharacterId(qPlayer.getPlayer());
                    if (characterId == null) return;

                    String completableType = getCompletableType(completable);
                    String completableId = getCompletableId(completable);

                    playerDao.saveCharacterObjective(
                        characterId,
                        completableType,
                        completableId,
                        stage.getId(),
                        objectiveId,
                        objectiveType,
                        activeObjective.getProgress(),
                        activeObjective.isCompleted(),
                        objectiveData
                    );
                } else if (holder instanceof QEvent event) {
                    playerDao.saveEventObjective(
                        event.getId(),
                        stage.getId(),
                        objectiveId,
                        objectiveType,
                        activeObjective.getProgress(),
                        activeObjective.isCompleted(),
                        objectiveData
                    );
                }
            } catch (Exception e) {
                QuestsXL.log("Failed to save objective progress: " + e.getMessage());
                e.printStackTrace();
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Void> loadObjectivesForCompletable(ObjectiveHolder holder, Completable completable) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (holder instanceof QPlayer qPlayer) {
                    UUID characterId = getCurrentCharacterId(qPlayer.getPlayer());
                    if (characterId == null) return;

                    String completableType = getCompletableType(completable);
                    String completableId = getCompletableId(completable);

                    var objectives = playerDao.getCharacterObjectivesForCompletable(characterId, completableType, completableId);
                    for (var objData : objectives) {
                        restoreActiveObjective(holder, completable, objData.stageId, objData.objectiveId, objData.objectiveType, objData.progress, objData.completed, objData.objectiveData);
                    }
                } else if (holder instanceof QEvent event) {
                    var objectives = playerDao.getEventObjectives(event.getId());
                    for (var objData : objectives) {
                        restoreActiveObjective(holder, completable, objData.stageId, objData.objectiveId, objData.objectiveType, objData.progress, objData.completed, objData.objectiveData);
                    }
                }
            } catch (Exception e) {
                QuestsXL.log("Failed to load objective progress: " + e.getMessage());
                e.printStackTrace();
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Void> removeObjectiveProgress(ActiveObjective activeObjective) {
        return CompletableFuture.runAsync(() -> {
            try {
                var holder = activeObjective.getHolder();
                var completable = activeObjective.getCompletable();
                var stage = activeObjective.getStage();
                var objective = activeObjective.getObjective();

                String objectiveId = generateObjectiveId(objective, stage);

                if (holder instanceof QPlayer qPlayer) {
                    UUID characterId = getCurrentCharacterId(qPlayer.getPlayer());
                    if (characterId == null) return;

                    String completableType = getCompletableType(completable);
                    String completableId = getCompletableId(completable);

                    playerDao.removeCharacterObjective(characterId, completableType, completableId, stage.getId(), objectiveId);
                } else if (holder instanceof QEvent event) {
                    playerDao.removeEventObjective(event.getId(), stage.getId(), objectiveId);
                }
            } catch (Exception e) {
                QuestsXL.log("Failed to remove objective progress: " + e.getMessage());
                e.printStackTrace();
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Void> savePlayerData(QPlayer qPlayer) {
        return CompletableFuture.runAsync(() -> {
            UUID characterId = getCurrentCharacterId(qPlayer.getPlayer());
            if (characterId == null) {
                return;
            }

            for (var entry : qPlayer.getActiveQuests().entrySet()) {
                var activeQuest = entry.getKey();
                var startedAt = entry.getValue();
                playerDao.saveActiveQuest(characterId, activeQuest.getQuest().getName(),
                    activeQuest.getCurrentStage().getId(), startedAt);
            }

            for (var entry : qPlayer.getCompletedQuests().entrySet()) {
                var quest = entry.getKey();
                var completedAt = entry.getValue();
                playerDao.addCompletedQuest(characterId, quest.getName(), completedAt);
            }

            for (var entry : qPlayer.getScores().entrySet()) {
                playerDao.setScore(characterId, entry.getKey(), entry.getValue());
            }

            if (qPlayer.getExplorer() != null) {
                String explorationJson = qPlayer.getExplorer().toJson().toString();
                playerDao.saveExplorationData(characterId, explorationJson);
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Void> loadPlayerData(QPlayer qPlayer) {
        return CompletableFuture.runAsync(() -> {
            UUID characterId = getCurrentCharacterId(qPlayer.getPlayer());
            if (characterId == null) {
                return;
            }

            var activeQuests = playerDao.getActiveQuests(characterId);
            for (var questData : activeQuests) {
                var quest = QuestsXL.get().getQuestManager().getByName(questData.questId);
                if (quest != null) {
                    var activeQuest = new ActiveQuest(qPlayer, quest, questData.currentStage);
                    qPlayer.getActiveQuests().put(activeQuest, questData.startedAt);
                    loadObjectivesForCompletable(qPlayer, quest).join();
                }
            }

            var completedQuests = playerDao.getCompletedQuests(characterId);
            for (var questData : completedQuests) {
                var quest = QuestsXL.get().getQuestManager().getByName(questData.questId);
                if (quest != null) {
                    qPlayer.getCompletedQuests().put(quest, questData.completedAt);
                }
            }

            var scores = playerDao.getScores(characterId);
            for (var scoreData : scores) {
                qPlayer.getScores().put(scoreData.scoreName, scoreData.scoreValue);
            }

            var explorationData = playerDao.getExplorationData(characterId);
            if (explorationData.isPresent()) {
                try {
                    var json = com.google.gson.JsonParser.parseString(explorationData.get()).getAsJsonObject();
                    qPlayer.setExplorer(PlayerExplorer.fromJson(qPlayer, json));
                } catch (Exception e) {
                    QuestsXL.log("Failed to load exploration data for character " + characterId + ": " + e.getMessage());
                }
            }
        }, asyncExecutor);
    }

    public UUID getCurrentCharacterId(Player player) {
        if (hecateDatabaseManager == null) {
            return null;
        }
        var character = hecateDatabaseManager.getCurrentCharacter(player);
        return character != null ? character.getCharacterID() : null;
    }

    public QPlayer getCurrentPlayer(Player player) {
        UUID characterId = getCurrentCharacterId(player);
        if (characterId == null) {
            return null;
        }
        return uuidqPlayerMap.computeIfAbsent(characterId, id -> new QPlayer(player));
    }

    public QPlayer getCurrentPlayerByUUID(UUID playerUUID) {
        if (hecateDatabaseManager == null) {
            return null;
        }
        HPlayer hPlayer = hecateDatabaseManager.getHPlayer(playerUUID);
        if (hPlayer == null || hPlayer.getSelectedCharacter() == null) {
            return null;
        }
        return uuidqPlayerMap.get(hPlayer.getSelectedCharacter().getCharacterID());
    }

    private String generateObjectiveId(QObjective<?> objective, QStage stage) {
        String objectiveKey = findObjectiveKeyInStage(objective, stage);
        if (objectiveKey != null) {
            return objectiveKey;
        }

        return objective.getClass().getSimpleName() + "_" + stage.getId() + "_" + objective.hashCode();
    }

    private String findObjectiveKeyInStage(QObjective<?> objective, QStage stage) {
        try {
            var completable = stage.getOwner();
            YamlConfiguration cfg = null;

            if (completable instanceof QQuest quest) {
                cfg = quest.cfg;
            } else if (completable instanceof QEvent event) {
                cfg = event.cfg;
            }

            if (cfg == null) {
                return null;
            }

            // Navigate to the stage's objectives section
            ConfigurationSection stagesSection = cfg.getConfigurationSection("stages");
            if (stagesSection == null) {
                return null;
            }

            ConfigurationSection stageSection = stagesSection.getConfigurationSection(String.valueOf(stage.getId()));
            if (stageSection == null) {
                return null;
            }

            ConfigurationSection objectivesSection = stageSection.getConfigurationSection("objectives");
            if (objectivesSection == null) {
                return null;
            }

            // Look through all objective configurations to find the one that matches our objective instance
            String targetObjectiveType = QRegistries.OBJECTIVES.getId(objective.getClass());

            for (String key : objectivesSection.getKeys(false)) {
                String objectiveType = null;

                // Check if it's a simple format: <type>: <config>
                if (QRegistries.OBJECTIVES.isValid(key) && objectivesSection.isString(key)) {
                    objectiveType = key;
                }
                else if (objectivesSection.isConfigurationSection(key)) {
                    ConfigurationSection objSection = objectivesSection.getConfigurationSection(key);
                    if (objSection != null) {
                        if (objSection.contains("type")) {
                            objectiveType = objSection.getString("type");
                        } else {
                            objectiveType = key; // Use the key as type if no explicit type
                        }
                    }
                }

                // If this matches our objective type, we found our key
                if (targetObjectiveType.equals(objectiveType)) {
                    return key;
                }
            }

        } catch (Exception e) {
            QuestsXL.log("Failed to find objective key in stage configuration: " + e.getMessage());
        }

        return null;
    }

    private String getCompletableType(Completable completable) {
        if (completable instanceof QQuest) {
            return "QUEST";
        } else if (completable instanceof QEvent) {
            return "EVENT";
        } else if (completable instanceof GlobalObjectives) {
            return "GLOBAL";
        }
        return "UNKNOWN";
    }

    private String getCompletableId(Completable completable) {
        if (completable instanceof QQuest quest) {
            return quest.getName();
        } else if (completable instanceof QEvent event) {
            return event.getId();
        } else if (completable instanceof GlobalObjectives) {
            return "global";
        }
        return "unknown";
    }

    private String serializeObjectiveData(ActiveObjective activeObjective) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("created_at", System.currentTimeMillis());
        return json.toString();
    }

    private void restoreActiveObjective(ObjectiveHolder holder, Completable completable, int stageId, String objectiveId, String objectiveType, int progress, boolean completed, String objectiveData) {
        try {
            var stage = completable.getStages().stream()
                .filter(s -> s.getId() == stageId)
                .findFirst()
                .orElse(null);

            if (stage == null) {
                QuestsXL.log("Could not find stage " + stageId + " for completable " + getCompletableId(completable));
                return;
            }

            QObjective<?> matchingObjective = findObjectiveByConfigKey(stage, objectiveId, objectiveType);

            if (matchingObjective == null) {
                QuestsXL.log("Could not find matching objective with ID '" + objectiveId + "' of type " + objectiveType + " in stage " + stageId);
                return;
            }

            ActiveObjective activeObjective = new ActiveObjective(holder, completable, stage, matchingObjective);
            activeObjective.setProgress(progress);
            activeObjective.setCompleted(completed);

            holder.addObjective(activeObjective);
            QuestsXL.get().getObjectiveEventManager().register(activeObjective);

            QuestsXL.log("Restored objective: " + objectiveType + " (ID: " + objectiveId + ") with progress " + progress + "/" + (completed ? "completed" : "active"));
        } catch (Exception e) {
            QuestsXL.log("Failed to restore objective: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Find an objective in a stage by its configuration key and type.
     * This handles both simple and complex objective configurations.
     */
    private QObjective<?> findObjectiveByConfigKey(QStage stage, String objectiveId, String objectiveType) {
        try {
            var completable = stage.getOwner();
            YamlConfiguration cfg = null;

            if (completable instanceof QQuest quest) {
                cfg = quest.cfg;
            } else if (completable instanceof QEvent event) {
                cfg = event.cfg;
            }

            if (cfg == null) {
                return fallbackObjectiveMatch(stage, objectiveType);
            }

            ConfigurationSection objectivesSection = cfg.getConfigurationSection("stages." + stage.getId() + ".objectives");
            if (objectivesSection == null) {
                return fallbackObjectiveMatch(stage, objectiveType);
            }

            String targetType = null;
            boolean isSimpleFormat = false;

            if (objectivesSection.contains(objectiveId)) {
                if (objectivesSection.isString(objectiveId)) {
                    // Simple format: <type>: <config>
                    targetType = objectiveId;
                    isSimpleFormat = true;
                } else if (objectivesSection.isConfigurationSection(objectiveId)) {
                    // Complex format with ID
                    ConfigurationSection objSection = objectivesSection.getConfigurationSection(objectiveId);
                    if (objSection != null) {
                        targetType = objSection.getString("type", objectiveId);
                    }
                }
            }

            if (!objectiveType.equals(targetType)) {
                QuestsXL.log("Objective ID '" + objectiveId + "' found but type mismatch. Expected: " + objectiveType + ", Found: " + targetType);
                return fallbackObjectiveMatch(stage, objectiveType);
            }
            // If we found a simple format, we can return the objective directly
            return findObjectiveInstanceByPosition(stage, objectiveId, objectiveType, objectivesSection);

        } catch (Exception e) {
            QuestsXL.log("Failed to find objective by config key: " + e.getMessage());
            return fallbackObjectiveMatch(stage, objectiveType);
        }
    }

    /**
     * Find the actual objective instance by matching the position in the configuration
     * with the position in the stage's goals list.
     */
    private QObjective<?> findObjectiveInstanceByPosition(QStage stage, String objectiveId, String objectiveType, ConfigurationSection objectivesSection) {
        try {
            // Get the position of our target objective in the configuration
            java.util.List<String> configKeys = new java.util.ArrayList<>(objectivesSection.getKeys(false));
            int targetPosition = -1;
            int currentPosition = 0;

            for (String key : configKeys) {
                String keyType = null;

                // Determine the type for this configuration key
                if (QRegistries.OBJECTIVES.isValid(key) && objectivesSection.isString(key)) {
                    keyType = key;
                } else if (objectivesSection.isConfigurationSection(key)) {
                    ConfigurationSection objSection = objectivesSection.getConfigurationSection(key);
                    if (objSection != null) {
                        keyType = objSection.getString("type", key);
                    }
                }

                if (key.equals(objectiveId) && objectiveType.equals(keyType)) {
                    targetPosition = currentPosition;
                    break;
                }
                currentPosition++;
            }

            if (targetPosition == -1) {
                return fallbackObjectiveMatch(stage, objectiveType);
            }

            List<QObjective> goals = new ArrayList<>(stage.getGoals());
            int currentGoalPosition = 0;

            for (QObjective<?> objective : goals) {
                String goalType = QRegistries.OBJECTIVES.getId(objective.getClass());
                if (objectiveType.equals(goalType)) {
                    if (currentGoalPosition == targetPosition) {
                        return objective;
                    }
                    currentGoalPosition++;
                }
            }

            return fallbackObjectiveMatch(stage, objectiveType);

        } catch (Exception e) {
            QuestsXL.log("Failed to find objective by position: " + e.getMessage());
            return fallbackObjectiveMatch(stage, objectiveType);
        }
    }

    private QObjective<?> fallbackObjectiveMatch(QStage stage, String objectiveType) {
        Class<?> objectiveClass = QRegistries.OBJECTIVES.get(objectiveType).getClass();
        if (objectiveClass == null) {
            return null;
        }

        for (QObjective<?> stageObjective : stage.getGoals()) {
            if (stageObjective.getClass().equals(objectiveClass)) {
                return stageObjective;
            }
        }

        return null;
    }

    public Set<QPlayer> getPlayers() {
        return new HashSet<>(uuidqPlayerMap.values());
    }
}
