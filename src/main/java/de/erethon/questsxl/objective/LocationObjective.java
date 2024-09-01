package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLocation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Arrays;

public class LocationObjective extends AbstractLocationBasedObjective {

    public QLocation getLocation() {
        return location;
    }

    public int getDistance() {
        return distance;
    }

    @Override
    public void check(ActiveObjective active, Event e) {
        if (!(e instanceof PlayerMoveEvent event)) return;
        if (!conditions(event.getPlayer())) return;
        if (location == null || !location.sameWorld(event.getTo())) {
            return;
        }
        if (event.getTo().distance(location.get(event.getTo())) <= distance) {
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(event.getPlayer()));
        }
    }

    @Override
    public void load(QLineConfig section) {
        super.load(section);
        location = new QLocation(section);
        distance = section.getInt("range", 2);
        if (distance <= 0) {
            throw new RuntimeException("The location objective in " + section + " contains a negative range.");
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        location = new QLocation(section);
        distance = section.getInt("range");
        if (distance <= 0) {
            throw new RuntimeException("The location objective in " + section.getName() + " contains a negative range.");
        }
    }
}
