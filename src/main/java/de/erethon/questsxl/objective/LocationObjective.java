package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
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
    public void load(QConfig cfg) {
        super.load(cfg);
        location = cfg.getQLocation("location");
        distance = cfg.getInt("range");
        if (distance <= 0) {
            throw new RuntimeException("The location objective in " + cfg.getName() + " contains a negative range.");
        }
    }
}
