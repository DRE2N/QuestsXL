package de.erethon.questsxl.condition;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.players.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
    public void load(ConfigurationSection section) {
        super.load(section);
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        if (world == null) {
            MessageUtil.log("The condition " + section.getName() + " contains a location for a world that is not loaded.");
            return;
        }
        location = new Location(Bukkit.getWorld(world), x, y, z);
        radius = section.getDouble("range");
    }

}
