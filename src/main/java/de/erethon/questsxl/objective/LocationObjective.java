package de.erethon.questsxl.objective;

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
        if (location == null) {
            return;
        }
        if (event.getTo().distance(location.get(event.getTo())) <= distance) {
            complete(active.getHolder(), this);
        }
    }

    @Override
    public void load(String[] c) {
        distance = Integer.parseInt(c[0]);
        if (distance <= 0) {
            throw new RuntimeException("The location objective in " + Arrays.toString(c) + " contains a negative range.");
        }
        location = new QLocation(c, 1);
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
