package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.EventState;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

@QLoadableDoc(
        value = "event_state",
        description = "Checks if the specified event is in the specified state.",
        shortExample = "'event_state: id=example_event; state=active'",
        longExample = {
                "event_state:",
                "  id: example_event",
                "  state: active",
        }
)
public class EventStateCondition extends QBaseCondition {

    QuestsXL plugin = QuestsXL.getInstance();

    @QParamDoc(name = "id", description = "The ID of the event.", required = true)
    QEvent event;
    @QParamDoc(name = "state", description = "The state the event should be in. One of `active`, `inactive`, `completed` or `disabled`", def = "active")
    EventState state;

    @Override
    public boolean check(QPlayer player) {
        if (event.getState() == state) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public boolean check(QEvent conditionEvent) {
        if (event.getState() == state) {
            return success(conditionEvent);
        }
        return fail(conditionEvent);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        event = plugin.getEventManager().getByID(cfg.getString("id"));
        state = EventState.valueOf(cfg.getString("state", "active").toUpperCase(Locale.ROOT));
    }

}
