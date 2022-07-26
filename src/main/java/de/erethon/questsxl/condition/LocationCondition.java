package de.erethon.questsxl.condition;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class LocationCondition extends QBaseCondition {

    private Location location;
    private double radius;

    @Override
    public boolean check(QPlayer player) {
        if (player.getPlayer().getLocation().distance(location) <= radius) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public boolean check(QEvent event) {
        if (event.getCenterLocation().distance(location) <= radius) {
            return success(event);
        }
        return fail(event);
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
        radius = section.getDouble("range");
    }

}
