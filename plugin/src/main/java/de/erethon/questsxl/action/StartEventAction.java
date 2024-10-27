package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "start_event",
        description = "Starts an event.",
        shortExample = "start_event: event=example_event",
        longExample = {
                "start_event:",
                "  event: example_event",
                "  skipConditions: true"
        }
)
public class StartEventAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();

    @QParamDoc(name = "event", description = "The ID of the event to start", required = true)
    QEvent event;
    @QParamDoc(name = "skipConditions", description = "Whether to skip the event's conditions", def = "false")
    boolean skipConditions = false;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        event.startFromAction(skipConditions);
    }

    @Override
    public void play(QEvent event) {
        if (!conditions(event)) return;
        event.startFromAction(skipConditions);
    }


    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        event = plugin.getEventManager().getByID(cfg.getString("event"));
        if (event == null) {
            throw new RuntimeException("Event " + cfg.getString("event") + " does not exist.");
        }
        skipConditions = cfg.getBoolean("skipConditions", false);
    }
}
