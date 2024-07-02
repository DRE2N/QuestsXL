package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;

public class LocationCondition extends QBaseCondition {

    private QLocation location;
    private double radius;

    @Override
    public boolean check(QPlayer player) {
        if (player.getPlayer().getLocation().distance(location.get(player.getLocation())) <= radius) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public boolean check(QEvent event) {
        if (event.getCenterLocation().distance(location.get(event.getCenterLocation())) <= radius) {
            return success(event);
        }
        return fail(event);
    }

    @Override
    public void load(QLineConfig section) {
        location = new QLocation(section);
        radius = section.getDouble("range");
        if (radius <= 0) {
            throw new RuntimeException("The location condition in " + section + " contains a negative radius.");
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        location = new QLocation(section);
        radius = section.getDouble("range");
        if (radius <= 0) {
            throw new RuntimeException("The location condition in " + section.getName() + " contains a negative radius.");
        }
    }

}
