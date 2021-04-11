package de.erethon.questsxl.objectives;

import de.erethon.questsxl.events.CommandTriggerEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

public class ServerCommandObjective extends QBaseObjective {

    String identifier;

    @Override
    public void check(Event e) {
        if (!(e instanceof CommandTriggerEvent)) return;
        CommandTriggerEvent event = (CommandTriggerEvent) e;
        if (identifier.equals(event.getID())) {
            complete(event.getPlayer(), this);
        }

    }

    @Override
    public void load(ConfigurationSection cfg) {
        super.load(cfg);
        identifier = cfg.getString("identifier");
    }
}
