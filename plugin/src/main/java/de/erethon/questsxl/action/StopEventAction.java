package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;

@QLoadableDoc(
        value = "stop_event",
        description = "Stops an event.",
        shortExample = "stop_event: event=example_event",
        longExample = {
                "stop_event:",
                "  event: example_event",
        }
)
public class StopEventAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.get();

    @QParamDoc(name = "event", description = "The ID of the event to stop", required = true)
    String eventId;

    @QParamDoc(name = "failed", description = "If true, the event will be marked as failed instead of completed", def = "false")
    boolean failed = false;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        QEvent event = plugin.getEventManager().getByID(eventId);
        if (event == null) {
            throw new RuntimeException("Event " + eventId + " does not exist.");
        }
        onFinish(quester);
        event.stop(failed);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        eventId = cfg.getString("event");
        failed = cfg.getBoolean("failed");
        if (eventId == null) {
            throw new RuntimeException("Event ID is missing.");
        }
    }
}
