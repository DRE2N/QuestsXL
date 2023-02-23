package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.EventState;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class EventStateCondition extends QBaseCondition {

    QuestsXL plugin = QuestsXL.getInstance();

    QEvent event;
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
    public void load(ConfigurationSection section) {
        super.load(section);
        event = plugin.getEventManager().getByID(section.getString("id"));
        state = EventState.valueOf(section.getString("state"));
    }

    @Override
    public void load(QLineConfig section) {
        event = plugin.getEventManager().getByID(section.getString("id"));
        state = EventState.valueOf(section.getString("state"));
    }
}
