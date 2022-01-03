package de.erethon.questsxl.objectives;

import de.erethon.aether.events.InstancedCreatureArriveAtPointEvent;
import de.erethon.aether.events.InstancedCreatureDeathEvent;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.ArrayList;
import java.util.List;

public class EscortNPCObjective extends QBaseObjective {

    List<Location> path = new ArrayList<>();

    @Override
    public void check(Event e) {
        if (e instanceof InstancedCreatureDeathEvent event) {
            for (Player player : event.getNpc().getViewers()) {
                fail(player, this);
            }
        }
        if (e instanceof InstancedCreatureArriveAtPointEvent event) {
            for (Player player : event.getNpc().getViewers()) {
                complete(player, this);
            }
        }
    }

}
