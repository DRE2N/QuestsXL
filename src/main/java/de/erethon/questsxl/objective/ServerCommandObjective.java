package de.erethon.questsxl.objective;

import de.erethon.questsxl.event.CommandTriggerEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

public class ServerCommandObjective extends QBaseObjective {

    String identifier;

    @Override
    public void check(Event e) {
        if (!(e instanceof CommandTriggerEvent event)) return;
        if (!conditions(event.getPlayer())) return;
        if (identifier.equals(event.getID())) {
            complete(plugin.getPlayerCache().getByPlayer(event.getPlayer()), this);
        }

    }

    @Override
    public void load(ConfigurationSection cfg) {
        super.load(cfg);
        identifier = cfg.getString("identifier");
    }
}
