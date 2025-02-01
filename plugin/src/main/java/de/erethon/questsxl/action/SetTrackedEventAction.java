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

    @QParamDoc(name = "event", description = "ID of the event to set as the tracked event.", required = true)
    private QEvent event;
    @QParamDoc(name = "priority", description = "Priority. Higher values equal a higher priority", def = "1")
    private int priority = 0;


    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester, (QPlayer player) -> player.setTrackedEvent(event, priority));
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        event = cfg.getQEvent("event");
        if (event == null) {
            QuestsXL.getInstance().addRuntimeError(new FriendlyError(cfg.getName(), "Event not found", "The event specified in the action could not be found.", "Check the event name in the action configuration."));
        }
        priority = cfg.getInt("priority", 1);
    }
}
