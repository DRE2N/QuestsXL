package de.erethon.questsxl.livingworld;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;

public class Exploration {

    public static final double MAX_DISTANCE_FOR_HINT = 64;

    private Set<ExplorationSet> sets = new HashSet<>();

    public ExplorationSet getSet(String id) {
        for (ExplorationSet set : sets) {
            if (set.id().equals(id)) {
                return set;
            }
        }
        return null;
    }

    public ExplorationSet getClosestSet(Location location) {
        ExplorationSet closest = null;
        double distance = Double.MAX_VALUE;
        for (ExplorationSet set : sets) {
            double d = set.averageLocation().distanceSquared(location);
            if (d < distance) {
                distance = d;
                closest = set;
            }
        }
        return closest;
    }

    public boolean hasAnyRelevantExplorables(Location location) {
        for (ExplorationSet set : sets) {
            if (set.averageLocation().distanceSquared(location) < MAX_DISTANCE_FOR_HINT * MAX_DISTANCE_FOR_HINT) {
                return true;
            }
        }
        return false;
    }


}
