package de.erethon.questsxl.player;

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.customizer.Bind;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface QPlayerDao {

    // Active Quest Operations
    @SqlQuery("SELECT quest_id, current_stage, started_at FROM q_character_active_quests WHERE character_id = ?")
    List<ActiveQuestData> getActiveQuests(UUID characterId);

    @SqlUpdate("INSERT INTO q_character_active_quests (character_id, quest_id, current_stage, started_at) VALUES (?, ?, ?, ?) ON CONFLICT (character_id, quest_id) DO UPDATE SET current_stage = EXCLUDED.current_stage")
    void saveActiveQuest(@Bind UUID characterId, @Bind String questId, @Bind int currentStage, @Bind long startedAt);

    @SqlUpdate("DELETE FROM q_character_active_quests WHERE character_id = ? AND quest_id = ?")
    void removeActiveQuest(UUID characterId, String questId);

    // Completed Quest Operations
    @SqlQuery("SELECT quest_id, completed_at FROM q_character_completed_quests WHERE character_id = ?")
    List<CompletedQuestData> getCompletedQuests(UUID characterId);

    @SqlUpdate("INSERT INTO q_character_completed_quests (character_id, quest_id, completed_at) VALUES (?, ?, ?) ON CONFLICT (character_id, quest_id) DO NOTHING")
    void addCompletedQuest(UUID characterId, String questId, long completedAt);

    // Score Operations
    @SqlQuery("SELECT score_name, score_value FROM q_character_scores WHERE character_id = ?")
    List<ScoreData> getScores(UUID characterId);

    @SqlUpdate("INSERT INTO q_character_scores (character_id, score_name, score_value) VALUES (?, ?, ?) ON CONFLICT (character_id, score_name) DO UPDATE SET score_value = EXCLUDED.score_value")
    void setScore(UUID characterId, String scoreName, int scoreValue);

    @SqlUpdate("DELETE FROM q_character_scores WHERE character_id = ? AND score_name = ?")
    void removeScore(UUID characterId, String scoreName);

    // Exploration Data Operations
    @SqlQuery("SELECT exploration_data FROM q_character_exploration WHERE character_id = ?")
    Optional<String> getExplorationData(UUID characterId);

    @SqlUpdate("INSERT INTO q_character_exploration (character_id, exploration_data) VALUES (?, ?) ON CONFLICT (character_id) DO UPDATE SET exploration_data = EXCLUDED.exploration_data, updated_at = CURRENT_TIMESTAMP")
    void saveExplorationData(UUID characterId, String explorationData);

    // Character Objective Operations
    @SqlQuery("SELECT completable_type, completable_id, stage_id, objective_id, objective_type, progress, completed, objective_data FROM q_character_objectives WHERE character_id = ?")
    List<ObjectiveProgressData> getCharacterObjectives(UUID characterId);

    @SqlQuery("SELECT completable_type, completable_id, stage_id, objective_id, objective_type, progress, completed, objective_data FROM q_character_objectives WHERE character_id = ? AND completable_type = ? AND completable_id = ?")
    List<ObjectiveProgressData> getCharacterObjectivesForCompletable(UUID characterId, String completableType, String completableId);

    @SqlUpdate("INSERT INTO q_character_objectives (character_id, completable_type, completable_id, stage_id, objective_id, objective_type, progress, completed, objective_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (character_id, completable_type, completable_id, stage_id, objective_id) DO UPDATE SET progress = EXCLUDED.progress, completed = EXCLUDED.completed, objective_data = EXCLUDED.objective_data, updated_at = CURRENT_TIMESTAMP")
    void saveCharacterObjective(UUID characterId, String completableType, String completableId, int stageId, String objectiveId, String objectiveType, int progress, boolean completed, String objectiveData);

    @SqlUpdate("DELETE FROM q_character_objectives WHERE character_id = ? AND completable_type = ? AND completable_id = ? AND stage_id = ? AND objective_id = ?")
    void removeCharacterObjective(UUID characterId, String completableType, String completableId, int stageId, String objectiveId);

    @SqlUpdate("DELETE FROM q_character_objectives WHERE character_id = ? AND completable_type = ? AND completable_id = ?")
    void removeAllCharacterObjectivesForCompletable(UUID characterId, String completableType, String completableId);

    // Loot Chest Operations
    @SqlUpdate("CREATE TABLE IF NOT EXISTS q_character_looted_chests (character_id UUID, chest_id VARCHAR(255), looted_at BIGINT, PRIMARY KEY (character_id, chest_id))")
    void createLootChestTable();

    @SqlQuery("SELECT chest_id FROM q_character_looted_chests WHERE character_id = ?")
    List<String> getLootedChests(UUID characterId);

    @SqlUpdate("INSERT INTO q_character_looted_chests (character_id, chest_id, looted_at) VALUES (?, ?, ?) ON CONFLICT (character_id, chest_id) DO NOTHING")
    void markChestLooted(UUID characterId, String chestId, long lootedAt);

    @SqlQuery("SELECT COUNT(*) > 0 FROM q_character_looted_chests WHERE character_id = ? AND chest_id = ?")
    boolean hasLootedChest(UUID characterId, String chestId);

    // Periodic Quest Operations
    @SqlQuery("SELECT quest_id, completed_at, bonus_claimed FROM q_character_periodic_progress WHERE character_id = ? AND quest_type = ?")
    List<PeriodicQuestProgressData> getPeriodicQuestProgress(UUID characterId, String questType);

    @SqlUpdate("INSERT INTO q_character_periodic_progress (character_id, quest_type, quest_id, completed_at, bonus_claimed) VALUES (?, ?, ?, ?, ?) ON CONFLICT (character_id, quest_type, quest_id) DO UPDATE SET completed_at = EXCLUDED.completed_at, bonus_claimed = EXCLUDED.bonus_claimed")
    void savePeriodicQuestProgress(UUID characterId, String questType, String questId, long completedAt, boolean bonusClaimed);

    @SqlUpdate("DELETE FROM q_character_periodic_progress WHERE character_id = ? AND quest_type = ?")
    void clearPeriodicQuestProgress(UUID characterId, String questType);

    @SqlUpdate("UPDATE q_character_periodic_progress SET bonus_claimed = ? WHERE character_id = ? AND quest_type = ?")
    void setPeriodicBonusClaimed(UUID characterId, String questType, boolean claimed);

    @SqlQuery("SELECT bonus_claimed FROM q_character_periodic_progress WHERE character_id = ? AND quest_type = ? LIMIT 1")
    Optional<Boolean> getPeriodicBonusClaimed(UUID characterId, String questType);

    // Periodic Quest State (global)
    @SqlQuery("SELECT last_daily_reset FROM q_periodic_quest_state WHERE id = 1")
    Optional<Long> getLastDailyReset();

    @SqlQuery("SELECT last_weekly_reset FROM q_periodic_quest_state WHERE id = 1")
    Optional<Long> getLastWeeklyReset();

    @SqlQuery("SELECT active_daily_quests FROM q_periodic_quest_state WHERE id = 1")
    Optional<String> getActiveDailyQuests();

    @SqlQuery("SELECT active_weekly_quests FROM q_periodic_quest_state WHERE id = 1")
    Optional<String> getActiveWeeklyQuests();

    @SqlUpdate("UPDATE q_periodic_quest_state SET last_daily_reset = ?, active_daily_quests = ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1")
    void updateDailyQuestState(long lastReset, String activeQuests);

    @SqlUpdate("UPDATE q_periodic_quest_state SET last_weekly_reset = ?, active_weekly_quests = ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1")
    void updateWeeklyQuestState(long lastReset, String activeQuests);

    @SqlUpdate("DELETE FROM q_character_periodic_progress WHERE quest_type = ?")
    void clearAllPlayersPeriodicProgress(String questType);

    // World Interaction Completion Operations
    @SqlQuery("SELECT COUNT(*) > 0 FROM q_character_completed_interactions WHERE character_id = ? AND interaction_id = ?")
    boolean hasCompletedInteraction(UUID characterId, String interactionId);

    @SqlUpdate("INSERT INTO q_character_completed_interactions (character_id, interaction_id, completed_at) VALUES (?, ?, ?) ON CONFLICT (character_id, interaction_id) DO NOTHING")
    void markInteractionCompleted(UUID characterId, String interactionId, long completedAt);

    @SqlQuery("SELECT interaction_id FROM q_character_completed_interactions WHERE character_id = ?")
    List<String> getCompletedInteractions(UUID characterId);

    // Data Transfer Objects
    class ActiveQuestData {
        public String questId;
        public int currentStage;
        public long startedAt;

        public ActiveQuestData() {}

        public ActiveQuestData(String questId, int currentStage, long startedAt) {
            this.questId = questId;
            this.currentStage = currentStage;
            this.startedAt = startedAt;
        }
    }

    class CompletedQuestData {
        public String questId;
        public long completedAt;

        public CompletedQuestData() {}

        public CompletedQuestData(String questId, long completedAt) {
            this.questId = questId;
            this.completedAt = completedAt;
        }
    }

    class ScoreData {
        public String scoreName;
        public int scoreValue;

        public ScoreData() {}

        public ScoreData(String scoreName, int scoreValue) {
            this.scoreName = scoreName;
            this.scoreValue = scoreValue;
        }
    }

    class ObjectiveProgressData {
        public String completableType;
        public String completableId;
        public int stageId;
        public String objectiveId;
        public String objectiveType;
        public int progress;
        public boolean completed;
        public String objectiveData;

        public ObjectiveProgressData() {}

        public ObjectiveProgressData(String completableType, String completableId, int stageId, String objectiveId, String objectiveType, int progress, boolean completed, String objectiveData) {
            this.completableType = completableType;
            this.completableId = completableId;
            this.stageId = stageId;
            this.objectiveId = objectiveId;
            this.objectiveType = objectiveType;
            this.progress = progress;
            this.completed = completed;
            this.objectiveData = objectiveData;
        }
    }

    class PeriodicQuestProgressData {
        public String questId;
        public long completedAt;
        public boolean bonusClaimed;

        public PeriodicQuestProgressData() {}

        public PeriodicQuestProgressData(String questId, long completedAt, boolean bonusClaimed) {
            this.questId = questId;
            this.completedAt = completedAt;
            this.bonusClaimed = bonusClaimed;
        }
    }
}
