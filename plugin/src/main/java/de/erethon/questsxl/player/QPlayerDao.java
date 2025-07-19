package de.erethon.questsxl.player;

import de.erethon.bedrock.jdbi.v3.sqlobject.statement.SqlQuery;
import de.erethon.bedrock.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Set;
import java.util.UUID;

public interface QPlayerDao {

    @SqlQuery("SELECT quest_id FROM q_player_quests WHERE character_id = ?")
    Set<String> getActiveQuests(UUID characterId);

    @SqlQuery("SELECT quest_id FROM q_player_completed_quests WHERE character_id = ?")
    Set<String> getCompletedQuests(UUID characterId);

    @SqlQuery("SELECT quest_id FROM q_player_quests WHERE character_id = ? AND quest_id = ?")
    void addActiveQuest(UUID characterId, String questId);

    @SqlQuery("SELECT quest_id FROM q_player_completed_quests WHERE character_id = ? AND quest_id = ?")
    void removeActiveQuest(UUID characterId, String questId);

    @SqlUpdate("INSERT INTO q_player_quests (character_id, quest_id) VALUES (?, ?)")
    void addCompletedQuest(UUID characterId, String questId);

}
