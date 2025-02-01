package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.EventState;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

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
public class EventStateCondition extends QBaseCondition {

    QuestsXL plugin = QuestsXL.getInstance();

    @QParamDoc(name = "event", description = "The ID of the event.")
    QEvent event;
    @QParamDoc(name = "state", description = "The state the event should be in. One of `active`, `inactive`, `completed` or `disabled`", def = "active")
    EventState state;

    @Override
    public boolean check(Quester quester) {
        if (quester instanceof QEvent e) {
            QEvent conditionEvent = event != null ? event : e;
            if (conditionEvent.getState() == state) {
                return success(e);
            }
        }
        if (quester instanceof QPlayer p) {
            if (event.getState() == state) {
                return success(p);
            }
        }
        return fail(quester);
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
