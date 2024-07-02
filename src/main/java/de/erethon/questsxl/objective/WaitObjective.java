package de.erethon.questsxl.objective;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QLineConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

public class WaitObjective extends QBaseObjective {

    long duration;
    // optional
    Location location;
    int distance;
    int durationWaited = 0;

    @Override
    public void onStart(ObjectiveHolder holder) {
        if (location == null) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> complete(holder, this), duration);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (holder.getLocation().distance(location) <= distance) {
                    if (++durationWaited >= duration) {
                        complete(holder, this);
                    }
                } else {
                    durationWaited = 0;
                }
            }, 20, 20);
        }
    }

    @Override
    public void check(ActiveObjective active, Event event) {

    }

    @Override
    public void load(QLineConfig section) {
        throw new RuntimeException("The wait objective does not support single-line configuration.");
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
            throw new RuntimeException("The wait objective in " + section.getName() + " contains a negative range.");
        }
    }
}
