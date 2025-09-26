package de.erethon.questsxl.livingworld;

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.customizer.Bind;

import java.util.List;
import java.util.Optional;

public interface QEventDao {

    // Event State Operations
    @SqlQuery("SELECT state, current_stage_id, time_last_completed, scores, event_participation FROM q_event_states WHERE event_id = ?")
    Optional<EventStateData> getEventState(String eventId);

    @SqlUpdate("INSERT INTO q_event_states (event_id, state, current_stage_id, time_last_completed, scores, event_participation) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (event_id) DO UPDATE SET state = EXCLUDED.state, current_stage_id = EXCLUDED.current_stage_id, time_last_completed = EXCLUDED.time_last_completed, scores = EXCLUDED.scores, event_participation = EXCLUDED.event_participation, updated_at = CURRENT_TIMESTAMP")
    void saveEventState(String eventId, String state, int currentStageId, long timeLastCompleted, String scores, String eventParticipation);

    @SqlUpdate("DELETE FROM q_event_states WHERE event_id = ?")
    void removeEventState(String eventId);

    // Event Objective Operations
    @SqlQuery("SELECT stage_id, objective_id, objective_type, progress, completed, objective_data FROM q_event_objectives WHERE event_id = ?")
    List<EventObjectiveProgressData> getEventObjectives(String eventId);

    @SqlUpdate("INSERT INTO q_event_objectives (event_id, stage_id, objective_id, objective_type, progress, completed, objective_data) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (event_id, stage_id, objective_id) DO UPDATE SET progress = EXCLUDED.progress, completed = EXCLUDED.completed, objective_data = EXCLUDED.objective_data, updated_at = CURRENT_TIMESTAMP")
    void saveEventObjective(String eventId, int stageId, String objectiveId, String objectiveType, int progress, boolean completed, String objectiveData);

    @SqlUpdate("DELETE FROM q_event_objectives WHERE event_id = ? AND stage_id = ? AND objective_id = ?")
    void removeEventObjective(String eventId, int stageId, String objectiveId);

    @SqlUpdate("DELETE FROM q_event_objectives WHERE event_id = ?")
    void removeAllEventObjectives(String eventId);

    // Data Transfer Objects
    class EventStateData {
        public String state;
        public int currentStageId;
        public long timeLastCompleted;
        public String scores;
        public String eventParticipation;

        public EventStateData() {}

        public EventStateData(String state, int currentStageId, long timeLastCompleted, String scores, String eventParticipation) {
            this.state = state;
            this.currentStageId = currentStageId;
            this.timeLastCompleted = timeLastCompleted;
            this.scores = scores;
            this.eventParticipation = eventParticipation;
        }
    }

    class EventObjectiveProgressData {
        public int stageId;
        public String objectiveId;
        public String objectiveType;
        public int progress;
        public boolean completed;
        public String objectiveData;

        public EventObjectiveProgressData() {}

        public EventObjectiveProgressData(int stageId, String objectiveId, String objectiveType, int progress, boolean completed, String objectiveData) {
            this.stageId = stageId;
            this.objectiveId = objectiveId;
            this.objectiveType = objectiveType;
            this.progress = progress;
            this.completed = completed;
            this.objectiveData = objectiveData;
        }
    }
}
