package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.event.CommandTriggerEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

public class ServerCommandObjective extends QBaseObjective {

    String identifier;

    @Override
    public void check(ActiveObjective objective, Event e) {
        if (!(e instanceof CommandTriggerEvent event)) return;
        if (!conditions(event.getPlayer())) return;
        if (identifier.equals(event.getID())) {
            checkCompletion(objective, this, plugin.getPlayerCache().getByPlayer(event.getPlayer()));
        }

    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        identifier = cfg.getString("identifier");
    }
}
