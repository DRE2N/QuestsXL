package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

import java.util.Map;

@QLoadableDoc(
        value = "event_range",
        description = "Checks if the objective is within the range of a specified event.",
        shortExample = "event_state: event=example_event; bonusRange=10",
        longExample = {
                "event_range:",
                "  event: example_event",
        }
)
public class EventRangeCondition extends QBaseCondition implements VariableProvider {

    private final QuestsXL plugin = QuestsXL.get();

    @QParamDoc(name = "event", description = "The ID of the event.")
    private String eventID;

    @QParamDoc(name = "bonusRange", description = "An additional range to add to the event's defined range.", def = "0")
    private double bonusRange;

    private double lastDistance = 0;

    @Override
    public boolean checkInternal(Quester quester) {
        QEvent event = plugin.getEventManager().getByID(eventID);
        if (eventID != null && event == null) {
            throw new RuntimeException("EventRangeCondition: Event with ID '" + eventID + "' not found.");
        }
        if (quester instanceof QEvent e) {
            QEvent conditionEvent = event != null ? event : e;
            lastDistance = conditionEvent.getLocation().distance(e.getLocation());
            if (lastDistance <= conditionEvent.getRange() + bonusRange) {
                return success(e);
            }
        }
        if (quester instanceof QPlayer p && findTopParent() instanceof QEvent e) {
            QEvent conditionEvent = event != null ? event : e;
            lastDistance = conditionEvent.getLocation().distance(p.getLocation());
            if (lastDistance <= conditionEvent.getRange() + bonusRange) {
                return success(p);
            }
        }
        return fail(quester);
    }

    /** Exposes %event_distance% (actual distance to the event centre) to child actions. */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("event_distance", new QVariable(lastDistance));
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
