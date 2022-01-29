package de.erethon.questsxl.objective;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;

public class LocationObjective extends AbstractLocationBasedObjective {

    public Location getLocation() {
        return location;
    }

    public int getDistance() {
        return distance;
    }

    @Override
    public void check(Event e) {
        if (!(e instanceof PlayerMoveEvent event)) return;
        if (!conditions(event.getPlayer())) return;
        if (location == null) {
            return;
        }
        if (event.getTo().distance(location) <= distance) {
            complete(event.getPlayer(), this);
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        if (world == null) {
            throw new RuntimeException("The location objective in " + section.getName() + " contains a location in an invalid world.");
        }
        location = new Location(Bukkit.getWorld(world), x, y, z);
        distance = section.getInt("range");
        if (distance <= 0) {
            throw new RuntimeException("The location objective in " + section.getName() + " contains a negative range.");
        }
    }
}
