package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "event_range",
        description = "Checks if the objective is within the range of a specified event.",
        shortExample = "event_state: event=example_event; bonusRange=10",
        longExample = {
                "event_range:",
                "  event: example_event",
        }
)
public class EventRangeCondition extends QBaseCondition {

    private final QuestsXL plugin = QuestsXL.get();

    @QParamDoc(name = "event", description = "The ID of the event.")
    private String eventID;

    @QParamDoc(name = "bonusRange", description = "An additional range to add to the event's defined range.", def = "0")
    private double bonusRange;

    @Override
    public boolean check(Quester quester) {
        QEvent event = plugin.getEventManager().getByID(eventID);
        if (eventID != null && event == null) {
            throw new RuntimeException("EventRangeCondition: Event with ID '" + eventID + "' not found.");
        }
        if (quester instanceof QEvent e) {
            QEvent conditionEvent = event != null ? event : e;
            if (conditionEvent.getLocation().distanceSquared(e.getLocation()) <= Math.pow(conditionEvent.getRange() + bonusRange, 2)) {
                return success(e);
            }
        }
        if (quester instanceof QPlayer p && findTopParent() instanceof QEvent e) {
            QEvent conditionEvent = event != null ? event : e;
            if (conditionEvent.getLocation().distanceSquared(p.getLocation()) <= Math.pow(conditionEvent.getRange() + bonusRange, 2)) {
                return success(p);
            }
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        if (cfg.contains("event")) {
            eventID = cfg.getString("event");
        }
        bonusRange = cfg.getDouble("bonusRange", 0);
    }
}
