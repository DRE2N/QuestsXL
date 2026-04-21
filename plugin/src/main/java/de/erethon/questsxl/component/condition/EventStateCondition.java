package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.livingworld.EventState;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

import java.util.Locale;
import java.util.Map;

@QLoadableDoc(
        value = "event_state",
        description = "Checks if the specified event is in the specified state.",
        shortExample = "event_state: event=example_event; state=active",
        longExample = {
                "event_state:",
                "  event: example_event",
                "  state: active",
        }
)
public class EventStateCondition extends QBaseCondition implements VariableProvider {

    QuestsXL plugin = QuestsXL.get();

    @QParamDoc(name = "event", description = "The ID of the event.")
    QEvent event;
    @QParamDoc(name = "state", description = "The state the event should be in. One of `active`, `inactive`, `completed` or `disabled`", def = "active")
    EventState state;

    private String lastEventState = "";
    private String lastEventId = "";

    @Override
    public boolean checkInternal(Quester quester) {
        if (quester instanceof QEvent e) {
            QEvent conditionEvent = event != null ? event : e;
            lastEventState = conditionEvent.getState().name().toLowerCase(Locale.ROOT);
            lastEventId = conditionEvent.getId();
            if (conditionEvent.getState() == state) {
                return success(e);
            }
        }
        if (quester instanceof QPlayer p) {
            QComponent top = findTopParent();
            QEvent resolvedEvent = event != null ? event : (top instanceof QEvent e2 ? e2 : null);
            if (resolvedEvent == null) return fail(quester);
            lastEventState = resolvedEvent.getState().name().toLowerCase(Locale.ROOT);
            lastEventId = resolvedEvent.getId();
            if (resolvedEvent.getState() == state) {
                return success(p);
            }
        }
        return fail(quester);
    }

    /** Exposes %event_state% and %event_id% to child actions (onSuccess / onFail). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of(
                "event_state", new QVariable(lastEventState),
                "event_id", new QVariable(lastEventId)
        );
    }


    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        if (cfg.contains("event")) {
            event = plugin.getEventManager().getByID(cfg.getString("event"));
        }
        state = EventState.valueOf(cfg.getString("state", "active").toUpperCase(Locale.ROOT));
    }

}
