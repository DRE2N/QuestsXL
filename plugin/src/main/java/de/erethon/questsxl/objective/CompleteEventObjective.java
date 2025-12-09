package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.event.QEventCompleteEvent;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "complete_event",
        description = "Completed when a specific event is completed by the player.",
        shortExample = "complete_event: event=dragon_slaying",
        longExample = {
                "complete_event:",
                "  event: 'dragon_slaying'",
                "  minParticipation: 50"
        }
)
public class CompleteEventObjective extends QBaseObjective<QEventCompleteEvent> {

    @QParamDoc(name = "event", description = "The ID of the event that needs to be completed to fulfill this objective.", required = true)
    private String eventID;
    @QParamDoc(name = "minParticipation", description = "The minimum participation percentage required to complete the objective.", def = "0")
    private int minParticipation = 0;

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("de=Schließe Event " + eventID + " ab; en=Complete event " + eventID);
    }

    @Override
    public Class<QEventCompleteEvent> getEventType() {
        return QEventCompleteEvent.class;
    }

    @Override
    public void check(ActiveObjective activeObjective, QEventCompleteEvent event) {
        if (!conditions(event.getPlayer())) {
            return;
        }
        if (eventID != null && !event.getQEvent().getId().equals(eventID)) {
            return;
        }
        if (event.getParticipation() < minParticipation) {
            return;
        }
        complete(event.getQPlayer(), this, event.getQEvent());
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        if (cfg.contains("event")) {
            eventID = cfg.getString("event");
        }
        minParticipation = cfg.getInt("minParticipation", 0);
        if (eventID == null) {
            plugin.addRuntimeError(new FriendlyError(findTopParent().id(), "Invalid event objective", "No event ID specified.", "Add a valid event ID."));
        }
    }
}
