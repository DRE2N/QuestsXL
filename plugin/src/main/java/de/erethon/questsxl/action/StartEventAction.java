package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;

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

    QuestsXL plugin = QuestsXL.get();

    @QParamDoc(name = "event", description = "The ID of the event to start", required = true)
    QEvent event;
    @QParamDoc(name = "skipConditions", description = "Whether to skip the event's conditions", def = "false")
    boolean skipConditions = false;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        event.startFromAction(skipConditions);
        onFinish(quester);
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
