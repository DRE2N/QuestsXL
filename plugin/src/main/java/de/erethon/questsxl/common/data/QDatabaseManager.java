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
import de.erethon.questsxl.instancing.InstanceDao;
import de.erethon.questsxl.livingworld.PlayerExplorer;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.livingworld.QEventDao;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerDao;
import de.erethon.questsxl.player.QPlayerDao.ActiveQuestData;
import de.erethon.questsxl.player.QPlayerDao.CompletedQuestData;
import de.erethon.questsxl.player.QPlayerDao.ScoreData;
import de.erethon.questsxl.player.QPlayerDao.ObjectiveProgressData;
import de.erethon.questsxl.livingworld.QEventDao.EventObjectiveProgressData;
import de.erethon.questsxl.livingworld.QEventDao.EventStateData;
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
    private final QEventDao eventDao;
    private final de.erethon.questsxl.instancing.InstanceDao instanceDao;

    public QDatabaseManager(BedrockDBConnection connection) {
        super(connection, new ThreadPoolExecutor(2, 4, 60L, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>()));
        hecate = Bukkit.getPluginManager().getPlugin("Hecate") instanceof Hecate ? (Hecate) Bukkit.getPluginManager().getPlugin("Hecate") : null;
        hecateDatabaseManager = hecate != null ? hecate.getDatabaseManager() : null;
        if (hecateDatabaseManager == null) {
            throw new IllegalStateException("Hecate database manager is not initialized.");
        }
        playerDao = getDao(QPlayerDao.class);
        eventDao = getDao(QEventDao.class);
        instanceDao = getDao(de.erethon.questsxl.instancing.InstanceDao.class);
    }

    @Override
    protected CompletableFuture<Void> initializeSchema() {
        return CompletableFuture.runAsync(() -> {
            try (var handle = jdbi.open()) {
                // First, create a schema version table if it doesn't exist
                handle.execute("""
                    CREATE TABLE IF NOT EXISTS q_schema_version (
                        version INTEGER NOT NULL,
                        applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (version)
                    )
                """);
                int currentVersion = handle.createQuery("SELECT COALESCE(MAX(version), 0) FROM q_schema_version")
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(0);

                QuestsXL.log("Current database schema version: " + currentVersion);
                applyMigrations(handle, currentVersion);

            } catch (Exception e) {
                QuestsXL.log("Failed to initialize schema: " + e.getMessage());
                e.printStackTrace();
            }
        }, asyncExecutor);
    }

    private void applyMigrations(org.jdbi.v3.core.Handle handle, int currentVersion) {
        if (currentVersion == 0) {
            boolean hasExistingTables = false;
            try {
                var result = handle.createQuery("""
                    SELECT COUNT(*) FROM information_schema.tables 
                    WHERE table_name IN ('q_character_active_quests', 'q_character_completed_quests', 'q_character_scores', 'q_character_objectives')
                    AND table_schema = current_schema()
                """).mapTo(Integer.class).findOne().orElse(0);

                hasExistingTables = result > 0;

                if (hasExistingTables) {
                    QuestsXL.log("Detected existing QuestsXL tables. Marking as migrated to avoid conflicts.");
                    // Mark migration 1 as complete since tables already exist
                    handle.execute("INSERT INTO q_schema_version (version) VALUES (1)");
                    currentVersion = 1;
                }
            } catch (Exception e) {
                QuestsXL.log("Could not check for existing tables, proceeding with normal migration: " + e.getMessage());
            }
        }

        if (currentVersion < 1) {
            QuestsXL.log("Applying migration 1: Initial schema");
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

            handle.execute("""
                CREATE TABLE IF NOT EXISTS q_character_completed_quests (
                    character_id UUID NOT NULL,
                    quest_id VARCHAR(255) NOT NULL,
                    completed_at BIGINT NOT NULL,
                    PRIMARY KEY (character_id, quest_id),
                    FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE
                )
            """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS q_character_scores (
                    character_id UUID NOT NULL,
                    score_name VARCHAR(255) NOT NULL,
                    score_value INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (character_id, score_name),
                    FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE
                )
            """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS q_character_exploration (
                    character_id UUID NOT NULL,
                    exploration_data TEXT NOT NULL,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (character_id),
                    FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE
                )
            """);

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

            handle.execute("INSERT INTO q_schema_version (version) VALUES (1)");
            QuestsXL.log("Applied migration 1: Initial schema");
        }

        if (currentVersion < 2) {
            QuestsXL.log("Applying migration 2: Event tables");
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

            handle.execute("""
                CREATE TABLE IF NOT EXISTS q_event_states (
                    event_id VARCHAR(255) NOT NULL,
                    state VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
                    current_stage_id INTEGER NOT NULL DEFAULT 0,
                    time_last_completed BIGINT NOT NULL DEFAULT 0,
                    scores TEXT,
                    event_participation TEXT,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (event_id)
                )
            """);

            handle.execute("INSERT INTO q_schema_version (version) VALUES (2)");
            QuestsXL.log("Applied migration 2: Event tables");
        }

        if (currentVersion < 3) {
            QuestsXL.log("Applying migration 3: Periodic quest tables");
            handle.execute("""
                CREATE TABLE IF NOT EXISTS q_periodic_quest_state (
                    id INTEGER PRIMARY KEY DEFAULT 1,
                    last_daily_reset BIGINT NOT NULL DEFAULT 0,
                    last_weekly_reset BIGINT NOT NULL DEFAULT 0,
                    active_daily_quests TEXT,
                    active_weekly_quests TEXT,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CHECK (id = 1)
                )
            """);

            handle.execute("""
                INSERT INTO q_periodic_quest_state (id, last_daily_reset, last_weekly_reset) 
                VALUES (1, 0, 0) 
                ON CONFLICT (id) DO NOTHING
            """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS q_character_periodic_progress (
                    character_id UUID NOT NULL,
                    quest_type VARCHAR(10) NOT NULL,
                    quest_id VARCHAR(255) NOT NULL,
                    completed_at BIGINT NOT NULL,
                    bonus_claimed BOOLEAN NOT NULL DEFAULT FALSE,
                    PRIMARY KEY (character_id, quest_type, quest_id),
                    FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE,
                    CHECK (quest_type IN ('DAILY', 'WEEKLY'))
                )
            """);

            handle.execute("INSERT INTO q_schema_version (version) VALUES (3)");
            QuestsXL.log("Applied migration 3: Periodic quest tables");
        }

        if (currentVersion < 4) {
            QuestsXL.log("Applying migration 4: World interaction completion tracking");
            handle.execute("""
                CREATE TABLE IF NOT EXISTS q_character_completed_interactions (
                    character_id UUID NOT NULL,
                    interaction_id VARCHAR(255) NOT NULL,
                    completed_at BIGINT NOT NULL,
                    PRIMARY KEY (character_id, interaction_id),
                    FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE
                )
            """);

            handle.execute("INSERT INTO q_schema_version (version) VALUES (4)");
            QuestsXL.log("Applied migration 4: World interaction completion tracking");
        }

        if (currentVersion < 5) {
            QuestsXL.log("Applying migration 5: Instance system tables");

            handle.execute("""
                CREATE TABLE IF NOT EXISTS q_instance_templates (
                    template_id VARCHAR(255) PRIMARY KEY,
                    world_name VARCHAR(255) NOT NULL,
                    min_x INT NOT NULL,
                    min_y INT NOT NULL,
                    min_z INT NOT NULL,
                    max_x INT NOT NULL,
                    max_y INT NOT NULL,
                    max_z INT NOT NULL,
                    block_data BYTEA NOT NULL,
                    block_entities BYTEA,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS q_character_instances (
                    character_id UUID NOT NULL,
                    template_id VARCHAR(255) NOT NULL,
                    modified_blocks BYTEA,
                    block_entities BYTEA,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (character_id, template_id),
                    FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE
                )
            """);

            handle.execute("INSERT INTO q_schema_version (version) VALUES (5)");
            QuestsXL.log("Applied migration 5: Instance system tables");
        }

        if (currentVersion < 6) {
            QuestsXL.log("Applying migration 6: Apartment rentals");

            handle.execute("""
                CREATE TABLE IF NOT EXISTS q_apartment_rentals (
                    character_id UUID NOT NULL,
                    template_id VARCHAR(255) NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (character_id, template_id),
                    FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE
                )
            """);

            handle.execute("INSERT INTO q_schema_version (version) VALUES (6)");
            QuestsXL.log("Applied migration 6: Apartment rentals");
        }

        if (currentVersion < 7) {
            QuestsXL.log("Applying migration 7: Apartment template chunks");

            handle.execute("""
                CREATE TABLE IF NOT EXISTS q_apartment_template_chunks (
                    template_id VARCHAR(255) NOT NULL,
                    world_name VARCHAR(255) NOT NULL,
                    chunk_x INT NOT NULL,
                    chunk_z INT NOT NULL,
                    PRIMARY KEY (template_id, world_name, chunk_x, chunk_z),
                    FOREIGN KEY (template_id) REFERENCES q_instance_templates(template_id) ON DELETE CASCADE
                )
            """);

            handle.execute("INSERT INTO q_schema_version (version) VALUES (7)");
            QuestsXL.log("Applied migration 7: Apartment template chunks");
        }

        QuestsXL.log("Database schema is up to date");
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

        jdbi.registerRowMapper(QPlayerDao.PeriodicQuestProgressData.class, (rs, ctx) -> {
            return new QPlayerDao.PeriodicQuestProgressData(
                rs.getString("quest_id"),
                rs.getLong("completed_at"),
                rs.getBoolean("bonus_claimed")
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

        jdbi.registerRowMapper(EventStateData.class, (rs, ctx) -> {
            return new EventStateData(
                rs.getString("state"),
                rs.getInt("current_stage_id"),
                rs.getLong("time_last_completed"),
                rs.getString("scores"),
                rs.getString("event_participation")
            );
        });

        // Instance system row mappers
        jdbi.registerRowMapper(InstanceDao.TemplateData.class, (rs, ctx) -> {
            var data = new InstanceDao.TemplateData();
            data.worldName = rs.getString("world_name");
            data.minX = rs.getInt("min_x");
            data.minY = rs.getInt("min_y");
            data.minZ = rs.getInt("min_z");
            data.maxX = rs.getInt("max_x");
            data.maxY = rs.getInt("max_y");
            data.maxZ = rs.getInt("max_z");
            data.blockData = rs.getBytes("block_data");
            data.blockEntities = rs.getBytes("block_entities");
            return data;
        });

        jdbi.registerRowMapper(InstanceDao.InstanceStateData.class, (rs, ctx) -> {
            var data = new InstanceDao.InstanceStateData();
            data.modifiedBlocks = rs.getBytes("modified_blocks");
            data.blockEntities = rs.getBytes("block_entities");
            return data;
        });

        jdbi.registerRowMapper(InstanceDao.TemplateChunk.class, (rs, ctx) -> {
            var data = new InstanceDao.TemplateChunk();
            data.templateId = rs.getString("template_id");
            data.worldName = rs.getString("world_name");
            data.chunkX = rs.getInt("chunk_x");
            data.chunkZ = rs.getInt("chunk_z");
            return data;
        });

        jdbi.registerRowMapper(de.erethon.questsxl.instancing.InstanceDao.RentalRow.class, (rs, ctx) -> {
            var data = new de.erethon.questsxl.instancing.InstanceDao.RentalRow();
            data.templateId = rs.getString("template_id");
            return data;
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
                    eventDao.saveEventObjective(
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
                    var objectives = eventDao.getEventObjectives(event.getId());
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
                    eventDao.removeEventObjective(event.getId(), stage.getId(), objectiveId);
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
                QuestsXL.log("Could not save data for player " + qPlayer.getPlayer().getName() + " - no character selected.");
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
            QuestsXL.log("Saved data for player " + qPlayer.getPlayer().getName() + " (Character ID: " + characterId + ")");
        }, asyncExecutor);
    }

    public CompletableFuture<Void> loadPlayerData(QPlayer qPlayer) {
        return CompletableFuture.supplyAsync(() -> {
            UUID characterId = getCurrentCharacterId(qPlayer.getPlayer());
            if (characterId == null) {
                return null;
            }

            // Load basic data synchronously
            var activeQuests = playerDao.getActiveQuests(characterId);
            var completedQuests = playerDao.getCompletedQuests(characterId);
            var scores = playerDao.getScores(characterId);
            var explorationData = playerDao.getExplorationData(characterId);

            return new Object[] { characterId, activeQuests, completedQuests, scores, explorationData };
        }, asyncExecutor).thenCompose(data -> {
            if (data == null) {
                return CompletableFuture.completedFuture(null);
            }

            UUID characterId = (UUID) data[0];
            var activeQuests = (java.util.List<ActiveQuestData>) data[1];
            var completedQuests = (java.util.List<CompletedQuestData>) data[2];
            var scores = (java.util.List<ScoreData>) data[3];
            var explorationData = (java.util.Optional<String>) data[4];

            // Process completed quests and scores first (no async dependencies)
            for (var questData : completedQuests) {
                var quest = QuestsXL.get().getQuestManager().getByName(questData.questId);
                if (quest != null) {
                    qPlayer.getCompletedQuests().put(quest, questData.completedAt);
                }
            }

            for (var scoreData : scores) {
                qPlayer.getScores().put(scoreData.scoreName, scoreData.scoreValue);
            }

            // Load exploration data
            if (explorationData.isPresent() && !explorationData.get().isEmpty()) {
                try {
                    String jsonString = explorationData.get();
                    if (!jsonString.equals("JsonObject") && jsonString.startsWith("{") && jsonString.endsWith("}")) {
                        var json = com.google.gson.JsonParser.parseString(jsonString).getAsJsonObject();
                        qPlayer.setExplorer(PlayerExplorer.fromJson(qPlayer, json));
                    } else {
                        QuestsXL.log("Invalid exploration data format for character " + characterId + ": " + jsonString);
                        qPlayer.setExplorer(new PlayerExplorer(qPlayer));
                    }
                } catch (Exception e) {
                    QuestsXL.log("Failed to load exploration data for character " + characterId + ": " + e.getMessage());
                    e.printStackTrace();
                    qPlayer.setExplorer(new PlayerExplorer(qPlayer));
                }
            } else {
                qPlayer.setExplorer(new PlayerExplorer(qPlayer));
            }

            // Load active quests and their objectives asynchronously without blocking
            List<CompletableFuture<Void>> questLoadFutures = new ArrayList<>();
            for (var questData : activeQuests) {
                var quest = QuestsXL.get().getQuestManager().getByName(questData.questId);
                if (quest != null) {
                    var activeQuest = new ActiveQuest(qPlayer, quest, questData.currentStage);
                    qPlayer.getActiveQuests().put(activeQuest, questData.startedAt);
                    // Don't block! Add to list of futures instead
                    questLoadFutures.add(loadObjectivesForCompletable(qPlayer, quest));
                }
            }

            // Wait for all quest objectives to load without blocking the thread pool
            return CompletableFuture.allOf(questLoadFutures.toArray(new CompletableFuture[0]));
        });
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
        QPlayer existing = uuidqPlayerMap.get(characterId);
        if (existing != null) {
            return existing;
        }

        // Create outside of computeIfAbsent to avoid deadlock
        // The QPlayer constructor calls loadFromDatabase which may trigger other code
        // that tries to access this same map, causing a deadlock in computeIfAbsent
        QPlayer newPlayer = new QPlayer(player);
        QPlayer result = uuidqPlayerMap.putIfAbsent(characterId, newPlayer);
        return result != null ? result : newPlayer;
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

    /**
     * Removes the QPlayer from the cache when the player disconnects.
     * This ensures stale player references are not kept around.
     */
    public void removePlayer(Player player) {
        UUID characterId = getCurrentCharacterId(player);
        if (characterId != null) {
            uuidqPlayerMap.remove(characterId);
        }
    }

    /**
     * Clears all QPlayers from the cache.
     * This should be called during plugin reload to ensure fresh player instances are created.
     */
    public void clearPlayers() {
        uuidqPlayerMap.clear();
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

    public CompletableFuture<Void> saveEventState(QEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String eventId = event.getId();
                String state = event.getState().name();
                int currentStageId = event.getCurrentStage() != null ? event.getCurrentStage().getId() : 0;
                long timeLastCompleted = event.getTimeLastCompleted();

                // Serialize scores and event participation
                String scoresJson = serializeScores(event.getScores());
                String participationJson = serializeEventParticipation(event.getParticipants());

                eventDao.saveEventState(eventId, state, currentStageId, timeLastCompleted, scoresJson, participationJson);

                QuestsXL.log("Saved state for event " + eventId + " (State: " + state + ", Stage: " + currentStageId + ")");
            } catch (Exception e) {
                QuestsXL.log("Failed to save event state for " + event.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Void> loadEventState(QEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String eventId = event.getId();
                var stateData = eventDao.getEventState(eventId);

                if (stateData.isPresent()) {
                    var data = stateData.get();

                    // Restore state
                    event.setState(de.erethon.questsxl.livingworld.EventState.valueOf(data.state));
                    event.setTimeLastCompleted(data.timeLastCompleted);

                    // Restore current stage
                    if (data.currentStageId > 0) {
                        event.setCurrentStage(data.currentStageId);
                    }

                    // Restore scores
                    if (data.scores != null) {
                        deserializeScores(data.scores, event.getScores());
                    }

                    // Restore event participation
                    if (data.eventParticipation != null) {
                        deserializeEventParticipation(data.eventParticipation, event.getParticipants());
                    }

                    QuestsXL.log("Loaded state for event " + eventId + " (State: " + data.state + ", Stage: " + data.currentStageId + ")");
                } else {
                    QuestsXL.log("No saved state found for event " + eventId + ", using default state");
                }

                return event;

            } catch (Exception e) {
                QuestsXL.log("Failed to load event state for " + event.getId() + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, asyncExecutor).thenCompose(evt -> {
            if (evt == null) {
                return CompletableFuture.completedFuture(null);
            }
            // Load objectives after state is restored, without blocking
            return loadObjectivesForCompletable(evt, evt);
        });
    }

    private String serializeScores(Map<String, Integer> scores) {
        try {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                json.addProperty(entry.getKey(), entry.getValue());
            }
            return json.toString();
        } catch (Exception e) {
            QuestsXL.log("Failed to serialize scores: " + e.getMessage());
            return "{}";
        }
    }

    private void deserializeScores(String scoresJson, Map<String, Integer> targetMap) {
        try {
            if (scoresJson == null || scoresJson.trim().isEmpty()) {
                return;
            }

            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(scoresJson).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
                targetMap.put(entry.getKey(), entry.getValue().getAsInt());
            }
        } catch (Exception e) {
            QuestsXL.log("Failed to deserialize scores: " + e.getMessage());
        }
    }

    private String serializeEventParticipation(Map<QPlayer, Integer> participation) {
        try {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            for (Map.Entry<QPlayer, Integer> entry : participation.entrySet()) {
                UUID characterId = getCurrentCharacterId(entry.getKey().getPlayer());
                if (characterId != null) {
                    json.addProperty(characterId.toString(), entry.getValue());
                }
            }
            return json.toString();
        } catch (Exception e) {
            QuestsXL.log("Failed to serialize event participation: " + e.getMessage());
            return "{}";
        }
    }

    private void deserializeEventParticipation(String participationJson, Map<QPlayer, Integer> targetMap) {
        try {
            if (participationJson == null || participationJson.trim().isEmpty()) {
                return;
            }

            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(participationJson).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
                try {
                    UUID characterId = UUID.fromString(entry.getKey());
                    QPlayer qPlayer = uuidqPlayerMap.get(characterId);

                    if (qPlayer != null) {
                        targetMap.put(qPlayer, entry.getValue().getAsInt());
                    }
                } catch (IllegalArgumentException e) {
                    QuestsXL.log("Invalid UUID in event participation data: " + entry.getKey());
                }
            }
        } catch (Exception e) {
            QuestsXL.log("Failed to deserialize event participation: " + e.getMessage());
        }
    }

    public QPlayerDao getPlayerDao() {
        return playerDao;
    }

    public de.erethon.questsxl.instancing.InstanceDao getInstanceDao() {
        return instanceDao;
    }

    // World Interaction Completion Methods

    /**
     * Checks if a character has completed a non-repeatable world interaction
     * @param characterId The character UUID
     * @param interactionId The interaction ID
     * @return true if the character has completed this interaction
     */
    public boolean hasCompletedInteraction(UUID characterId, String interactionId) {
        try {
            return playerDao.hasCompletedInteraction(characterId, interactionId);
        } catch (Exception e) {
            QuestsXL.log("Failed to check interaction completion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Marks a world interaction as completed for a character
     * @param characterId The character UUID
     * @param interactionId The interaction ID
     */
    public void markInteractionCompleted(UUID characterId, String interactionId) {
        CompletableFuture.runAsync(() -> {
            try {
                playerDao.markInteractionCompleted(characterId, interactionId, System.currentTimeMillis());
                QuestsXL.log("Marked interaction " + interactionId + " as completed for character " + characterId);
            } catch (Exception e) {
                QuestsXL.log("Failed to mark interaction as completed: " + e.getMessage());
                e.printStackTrace();
            }
        }, asyncExecutor);
    }
}
