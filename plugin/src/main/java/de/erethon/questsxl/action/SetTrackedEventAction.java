package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "set_tracked_event",
        description = "Sets the event that the player is currently tracking (e.g. shown in the sidebar). Priority is used to determine which event is shown if multiple actions are active.",
        shortExample = "set_tracked_event: event=example_event",
        longExample = {
                "set_tracked_event:",
                "  event: example_event",
                "  priority: 7"
        }
)
public class SetTrackedEventAction extends QBaseAction {

    @QParamDoc(name = "event", description = "ID of the event to set as the tracked event. Use `clear` to clear the tracked event.")
    private QEvent event = null;
    @QParamDoc(name = "priority", description = "Priority. Higher values equal a higher priority", def = "1")
    private int priority = 0;

    private boolean clear = false;


    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        if (clear) {
            execute(quester, (QPlayer player) -> player.setTrackedEvent(null, 99999));
        }
        if (event == null && findTopParent() instanceof QEvent e) {
            this.event = e;
        }
        execute(quester, (QPlayer player) -> player.setTrackedEvent(event, priority));
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        if (cfg.getString("event").equalsIgnoreCase("clear")) {
            clear = true;
            event = null;
            return;
        }
        event = cfg.getQEvent("event");
        priority = cfg.getInt("priority", 1);
    }
}
