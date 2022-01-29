package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
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
        return event.getState() == state;
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        event = plugin.getEventManager().getByID(section.getString("id"));
        state = EventState.valueOf(section.getString("state"));
    }

    @Override
    public void load(String[] c) {
        event = plugin.getEventManager().getByID(c[0]);
        state = EventState.valueOf(c[1].toUpperCase());
    }
}
