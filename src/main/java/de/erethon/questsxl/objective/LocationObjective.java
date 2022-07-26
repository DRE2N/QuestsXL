package de.erethon.questsxl.objective;

import de.erethon.bedrock.chat.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
            complete(plugin.getPlayerCache().getByPlayer(event.getPlayer()), this);
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        String worldName = section.getString("world", "Erethon");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            MessageUtil.log("The condition " + section.getName() + " contains a location for a world that is not loaded: " + worldName);
            return;
        }
        location = new Location(world, x, y, z);
        distance = section.getInt("range");
        if (distance <= 0) {
            throw new RuntimeException("The location objective in " + section.getName() + " contains a negative range.");
        }
    }
}
