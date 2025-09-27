package de.erethon.questsxl.livingworld;

import de.erethon.questsxl.livingworld.explorables.LootChest;
import de.erethon.questsxl.livingworld.explorables.PointOfInterest;
import de.erethon.questsxl.livingworld.explorables.ExplorableRespawnPoint;
import org.bukkit.Location;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Exploration {

    private final Set<ExplorationSet> sets = new HashSet<>();
    private final Set<PointOfInterest> pointsOfInterest = new HashSet<>();
    private final Set<LootChest> lootChests = new HashSet<>();
    private final Set<ExplorableRespawnPoint> explorableRespawnPoints = new HashSet<>();

    public void addSet(ExplorationSet set) {
        if (set != null) {
            sets.add(set);
        }
    }

    public void removeSet(ExplorationSet set) {
        if (set != null) {
            sets.remove(set);
        }
    }

    public Set<ExplorationSet> getSets() {
        return new HashSet<>(sets);
    }

    public void addPointOfInterest(PointOfInterest poi) {
        if (poi != null) {
            pointsOfInterest.add(poi);
        }
    }

    public void removePointOfInterest(PointOfInterest poi) {
        if (poi != null) {
            pointsOfInterest.remove(poi);
        }
    }

    public Set<PointOfInterest> getPointsOfInterest() {
        return new HashSet<>(pointsOfInterest);
    }

    public PointOfInterest getPointOfInterest(Location loc) {
        for (PointOfInterest poi : pointsOfInterest) {
            if (poi.location().distanceSquared(loc) < 1) {
                return poi;
            }
        }
        return null;
    }

    public PointOfInterest getPointOfInterest(String id) {
        for (PointOfInterest poi : pointsOfInterest) {
            if (poi.id().equals(id)) {
                return poi;
            }
        }
        return null;
    }

    public void addLootChest(LootChest chest) {
        if (chest != null) {
            lootChests.add(chest);
        }
    }

    public void removeLootChest(LootChest chest) {
        if (chest != null) {
            lootChests.remove(chest);
        }
    }

    public Set<LootChest> getLootChests() {
        return new HashSet<>(lootChests);
    }

    public LootChest getLootChest(String id) {
        for (LootChest chest : lootChests) {
            if (chest.id().equals(id)) {
                return chest;
            }
        }
        return null;
    }

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


    public Set<String> getPointOfInterestIDs() {
        Set<String> ids = new HashSet<>();
        for (PointOfInterest poi : pointsOfInterest) {
            ids.add(poi.id());
        }
        return ids;
    }

    public Set<String> getSetIDs() {
        Set<String> ids = new HashSet<>();
        for (ExplorationSet set : sets) {
            ids.add(set.id());
        }
        return ids;
    }

    public Set<String> getLootChestIDs() {
        Set<String> ids = new HashSet<>();
        for (LootChest chest : lootChests) {
            ids.add(chest.id());
        }
        return ids;
    }

    public void addExplorableRespawnPoint(ExplorableRespawnPoint respawnPoint) {
        if (respawnPoint != null) {
            explorableRespawnPoints.add(respawnPoint);
        }
    }

    public void removeExplorableRespawnPoint(ExplorableRespawnPoint respawnPoint) {
        if (respawnPoint != null) {
            explorableRespawnPoints.remove(respawnPoint);
        }
    }

    public Set<ExplorableRespawnPoint> getExplorableRespawnPoints() {
        return new HashSet<>(explorableRespawnPoints);
    }

    public ExplorableRespawnPoint getExplorableRespawnPoint(String id) {
        for (ExplorableRespawnPoint respawnPoint : explorableRespawnPoints) {
            if (respawnPoint.id().equals(id)) {
                return respawnPoint;
            }
        }
        return null;
    }

    public Set<String> getExplorableRespawnPointIDs() {
        Set<String> ids = new HashSet<>();
        for (ExplorableRespawnPoint respawnPoint : explorableRespawnPoints) {
            ids.add(respawnPoint.id());
        }
        return ids;
    }
}
