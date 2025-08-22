package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
enum RUN_MODE {
    EVENT_IN_RANGE,
    EVENT_PARTICIPANTS,
    ONLINE,
}

@QLoadableDoc(
        value = "run_as",
        description = "Runs a list of actions as a player. Useful for running actions on all players in an event. Optionally filters players by range or participation count.",
        shortExample = "<no short example>",
        longExample = {
                "run_as:",
                "  mode: event_in_range",
                "  value: 5",
                "  actions:",
                "    - 'message: message=Yeet'",
        }
)
public class RunAsAction extends QBaseAction {

    @QParamDoc(name = "mode", description = "The mode in which the action should be run. One of `event_in_range`, `event_participants` or `online`", def = "`online`")
    private RUN_MODE runMode = RUN_MODE.ONLINE;
    @QParamDoc(name = "value", description = "The value to filter players by. For `event_participants`, this is the min. participation. For `event_in_range`, this is the range in blocks *added* to the event range.", def = "0")
    private int runValue;
    @QParamDoc(name = "actions", description = "The list of actions to execute", required = true)
    private Set<QAction> actions;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        if (quester instanceof QPlayer player) {
            run(player); // This is useless but whatever
        }
        if (quester instanceof QEvent event) {
            run(event);
        }
        onFinish(quester);
    }

    private void run(QEvent event) {
        if (!conditions(event)) return;
        switch (runMode) {
            case EVENT_IN_RANGE:
                event.getLocation().getNearbyPlayers(event.getRange() + runValue).forEach(p -> run(databaseManager.getCurrentPlayer(p)));
                break;
            case EVENT_PARTICIPANTS:
                for (Map.Entry<QPlayer, Integer> entry : event.getParticipants().entrySet()) {
                    if (entry.getValue() >= runValue) {
                        run(entry.getKey());
                    }
                }
                break;
            case ONLINE:
                for (Player player : Bukkit.getOnlinePlayers()) {
                    run(databaseManager.getCurrentPlayer(player));
                }
                break;
        }
        onFinish(event);
    }

    private void run(QPlayer player)  {
        for (QAction action : actions) {
            action.play(player);
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        runMode = RUN_MODE.valueOf(cfg.getString("mode", "online").toUpperCase());
        runValue = cfg.getInt("value", 0);
        try {
            actions = cfg.getActions(this, "actions");
            for (QAction action : actions) {
                action.setParent(this);
            }
            if (actions.isEmpty()) {
                throw new RuntimeException("Action list is empty.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load actions: " + e.getMessage());
        }
    }

}

