package de.erethon.questsxl.objectives;

import de.erethon.commons.chat.MessageUtil;
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
    public String getDisplayText() {
        return "Go to " + location.toString();
    }

    @Override
    public void check(Event e) {
        if (!(e instanceof PlayerMoveEvent)) return;
        PlayerMoveEvent event = (PlayerMoveEvent) e;
        if (event.getTo().distance(location) < distance) {
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
            MessageUtil.log("The Objective " + section.getName() + " contains a teleport for a world that is not loaded.");
            return;
        }
        location = new Location(Bukkit.getWorld(world), x, y, z);
        distance = section.getInt("range");
    }
}
