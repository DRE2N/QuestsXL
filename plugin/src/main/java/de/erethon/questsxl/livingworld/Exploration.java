package de.erethon.questsxl.livingworld;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.explorables.LootChest;
import de.erethon.questsxl.livingworld.explorables.PointOfInterest;
import de.erethon.questsxl.respawn.RespawnPoint;
import de.erethon.questsxl.respawn.RespawnPointUnlockMode;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public class Exploration {

    private final QuestsXL plugin = QuestsXL.get();
    private final Set<ExplorationSet> sets = new HashSet<>();
    private final Set<PointOfInterest> pointsOfInterest = new HashSet<>();
    private final Set<LootChest> lootChests = new HashSet<>();
    private final Set<Explorable> standaloneExplorables = new HashSet<>();

    private BukkitTask centralVfxTask;
    private boolean vfxEnabled = true;

    /**
     * Clears all exploration data. Used for proper reload handling.
     */
    public void clearAll() {
        sets.clear();
        pointsOfInterest.clear();
        lootChests.clear();
        standaloneExplorables.clear();
    }

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
            Location avgLoc = set.averageLocation();
            if (avgLoc == null) continue; // Skip sets with no entries
            double d = avgLoc.distanceSquared(location);
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

    // New methods for standalone explorables (including respawn points that implement Explorable directly)
    public void addStandaloneExplorable(Explorable explorable) {
        if (explorable != null) {
            standaloneExplorables.add(explorable);
        }
    }

    public void removeStandaloneExplorable(Explorable explorable) {
        if (explorable != null) {
            standaloneExplorables.remove(explorable);
        }
    }

    public Set<Explorable> getStandaloneExplorables() {
        return new HashSet<>(standaloneExplorables);
    }

    public Explorable getStandaloneExplorable(String id) {
        for (Explorable explorable : standaloneExplorables) {
            if (explorable.id().equals(id)) {
                return explorable;
            }
        }
        return null;
    }

    /**
     * Gets all explorables (from sets and standalone) for proximity checks and content guide
     */
    public Set<Explorable> getAllExplorables() {
        Set<Explorable> allExplorables = new HashSet<>();

        // Add all explorables from sets
        for (ExplorationSet set : sets) {
            allExplorables.addAll(set.entries());
        }

        // Add standalone explorables
        allExplorables.addAll(standaloneExplorables);

        return allExplorables;
    }

    public ExplorationSet getSetContaining(Explorable explorable) {
        if (explorable == null) {
            return null;
        }
        for (ExplorationSet set : sets) {
            if (set.entries().contains(explorable)) {
                return set;
            }
        }
        return null;
    }

    /**
     * Starts the centralized VFX task that manages visual effects for all RespawnPoints
     */
    private void startCentralVFX() {
        if (centralVfxTask != null) {
            centralVfxTask.cancel();
        }

        centralVfxTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!vfxEnabled) {
                    return;
                }
                updateRespawnPointVFX();
            }
        }.runTaskTimer(plugin, 20L, 2L); // Start after 1 second, update every 2 ticks
    }

    /**
     * Updates VFX for all RespawnPoints that need visual effects
     */
    private void updateRespawnPointVFX() {
        Set<RespawnPoint> respawnPoints = getAllRespawnPoints();

        for (RespawnPoint respawnPoint : respawnPoints) {
            // Only process respawn points that should have VFX (NEAR and ACTION modes)
            if (!shouldHaveVFX(respawnPoint)) {
                continue;
            }

            Location loc = respawnPoint.location();
            if (loc == null || loc.getWorld() == null) {
                continue;
            }

            Chunk chunk = loc.getChunk();
            if (!chunk.isLoaded()) {
                respawnPoint.hideAllDisplays();
                continue;
            }

            // Check for players within visual range (32 blocks)
            boolean hasNearbyPlayers = false;
            for (Player player : loc.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(loc) <= 32 * 32) {
                    hasNearbyPlayers = true;
                    break;
                }
            }

            if (hasNearbyPlayers) {
                respawnPoint.updateVFX();
            } else {
                respawnPoint.hideAllDisplays();
            }
        }
    }

    private Set<RespawnPoint> getAllRespawnPoints() {
        Set<RespawnPoint> respawnPoints = new HashSet<>();

        // Get all respawn points directly from the RespawnPointManager
        if (plugin.getRespawnPointManager() != null) {
            for (RespawnPoint point : plugin.getRespawnPointManager().getRespawnPoints()) {
                if (shouldHaveVFX(point)) {
                    respawnPoints.add(point);
                }
            }
        }

        return respawnPoints;
    }

    /**
     * Determines if a RespawnPoint should have VFX based on its unlock mode
     */
    private boolean shouldHaveVFX(RespawnPoint respawnPoint) {
        RespawnPointUnlockMode mode = respawnPoint.getUnlockMode();
        return mode == RespawnPointUnlockMode.NEAR || mode == RespawnPointUnlockMode.ACTION;
    }

    /**
     * Initializes the centralized VFX system - should be called after all managers are loaded
     */
    public void initializeVFX() {
        if (vfxEnabled) {
            startCentralVFX();
        }
    }
}
