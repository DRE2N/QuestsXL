package de.erethon.questsxl.objective;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

public class WaitObjective extends QBaseObjective {

    long duration;
    // optional
    QLocation location;
    int distance;
    int durationWaited = 0;

    @Override
    public void onStart(ObjectiveHolder holder) {
        if (location == null) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> complete(holder, this), duration);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (holder.getLocation().distance(location.get(holder.getLocation())) <= distance) {
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
    public void load(QConfig cfg) {
        super.load(cfg);
        duration = cfg.getLong("duration");
        if (duration >= 0) {
            throw new RuntimeException("The wait objective in " + cfg.getName() + " contains a negative duration.");
        }
        location = cfg.getQLocation("location");
        distance = cfg.getInt("range");
        if (distance <= 0) {
            throw new RuntimeException("The wait objective in " + cfg.getName() + " contains a negative range.");
        }
    }
}
