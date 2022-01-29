package de.erethon.questsxl.objective;

import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class WaitObjective extends QBaseObjective {

    long duration;
    // optional
    Location location;
    int distance;
    int durationWaited = 0;

    @Override
    public void onStart(QPlayer player) {
        if (location == null) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> complete(player.getPlayer(), this), duration);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (player.getPlayer().getLocation().distance(location) <= distance) {
                    if (++durationWaited >= duration) {
                        complete(player.getPlayer(), this);
                    }
                } else {
                    durationWaited = 0;
                }
            }, 20, 20);
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        duration = section.getLong("duration");
        if (duration >= 0) {
            throw new RuntimeException("The wait objective in " + section.getName() + " contains a negative duration.");
        }
        ConfigurationSection locationSection = section.getConfigurationSection("location");
        if (locationSection == null) {
            return;
        }
        String world = locationSection.getString("world");
        double x = locationSection.getDouble("x");
        double y = locationSection.getDouble("y");
        double z = locationSection.getDouble("z");
        if (world == null) {
            throw new RuntimeException("The location objective in " + section.getName() + " contains a location in an invalid world.");
        }
        location = new Location(Bukkit.getWorld(world), x, y, z);
        distance = section.getInt("range");
        if (distance <= 0) {
            throw new RuntimeException("The wait objective in " + section.getName() + " contains a negative range.");
        }
    }
}
