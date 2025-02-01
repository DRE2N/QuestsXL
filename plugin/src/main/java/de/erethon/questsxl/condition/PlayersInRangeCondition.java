package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

enum Mode {
    IN_RANGE_EVENT,
    PARTICIPATING,
    IN_RANGE_PLAYER
}

@QLoadableDoc(
        value = "players_in_range",
        description = "Checks if the specified event or player has a certain amount of players in range.",
        shortExample = "players_in_range: event=example_event; min_players=1; max_players=5; mode=in_range_event",
        longExample = {
                "players_in_range:",
                "  min_players: 1",
                "  max_players: 5",
                "  mode: in_range_player",
                "  range: 16"
        }
)
public class PlayersInRangeCondition extends QBaseCondition {

    private final QuestsXL plugin = QuestsXL.getInstance();

    @QParamDoc(name = "event", description = "The ID of the event.")
    private QEvent event;

    @QParamDoc(name = "min_players", description = "The minimum amount of players required.", def = "0", required = true)
    private int minPlayers;

    @QParamDoc(name = "max_players", description = "The maximum amount of players required.", def = "0", required = true)
    private int maxPlayers;

    @QParamDoc(name = "mode", description = "The mode to check for. Can be `in_range_even`, `in_range_player` or `participating`", def = "`in_range_event`")
    private Mode mode;

    @QParamDoc(name = "range", description = "The range to check for players when mode `in_range_player` is used.", def = "32")
    private int range;

    @Override
    public boolean check(Quester quester) {
        if (event == null && mode == Mode.PARTICIPATING) {
            throw new RuntimeException("EventPlayersCondition has no event set but is called from a player.");
        }
        if (mode == Mode.PARTICIPATING || mode == Mode.IN_RANGE_EVENT) {
            checkInRange(event, quester);
        }
        if (mode == Mode.IN_RANGE_PLAYER && quester instanceof QPlayer player) {
            int players = player.getPlayer().getLocation().getNearbyPlayers(range).size();
            if (players >= minPlayers && players <= maxPlayers) {
                return success(quester);
            }
        }
        return fail(quester);
    }

    private void checkInRange(QEvent e, Quester quester) {
        QEvent conditionEvent = event != null ? event : e;
        if (mode == Mode.IN_RANGE_EVENT) {
            if (conditionEvent.getPlayersInRange().size() >= minPlayers && conditionEvent.getPlayersInRange().size() <= maxPlayers) {
                success(quester);
            }
        } else if (mode == Mode.PARTICIPATING) {
            if (conditionEvent.getParticipants().keySet().size() >= minPlayers && conditionEvent.getParticipants().keySet().size() <= maxPlayers) {
                success(quester);
            }
        }
        fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        if (cfg.contains("event")) {
            event = plugin.getEventManager().getByID(cfg.getString("event"));
        }
        minPlayers = cfg.getInt("min_players", 0);
        maxPlayers = cfg.getInt("max_players", 0);
        mode = Mode.valueOf(cfg.getString("mode", "in_range").toUpperCase());
        range = cfg.getInt("range", 32);
    }
}
