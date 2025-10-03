package de.erethon.questsxl.livingworld;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.explorables.LootChest;
import de.erethon.questsxl.livingworld.explorables.PointOfInterest;
import de.erethon.questsxl.respawn.RespawnPoint;
import de.erethon.questsxl.respawn.RespawnPointUnlockMode;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Exploration {

    private final QuestsXL plugin = QuestsXL.get();
    private final Set<ExplorationSet> sets = new HashSet<>();
    private final Set<PointOfInterest> pointsOfInterest = new HashSet<>();
    private final Set<LootChest> lootChests = new HashSet<>();
    private final Set<Explorable> standaloneExplorables = new HashSet<>();

    private BukkitTask centralVfxTask;
    private boolean vfxEnabled = true;

    private final Map<String, Set<RespawnPoint>> respawnPointsByWorld = new ConcurrentHashMap<>();
    private final Map<String, PlayerVFXData> playerVFXCache = new ConcurrentHashMap<>();
    private final Set<RespawnPoint> activeRespawnPoints = ConcurrentHashMap.newKeySet();
    private final int VFX_RANGE_SQUARED = 32 * 32;
    private final int VFX_CHUNK_RANGE = 3;
    private long lastVFXUpdate = 0;
    private final long VFX_UPDATE_INTERVAL = 100;

    // Cache player data to avoid repeated calculations
    private static class PlayerVFXData {
        final Location location;
        final String worldName;
        final int chunkX;
        final int chunkZ;
        final long timestamp;

        PlayerVFXData(Player player) {
            this.location = player.getLocation().clone();
            this.worldName = location.getWorld().getName();
            this.chunkX = location.getBlockX() >> 4;
            this.chunkZ = location.getBlockZ() >> 4;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Clears all exploration data. Used for proper reload handling.
     */
    public void clearAll() {
        stopVFX();

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
        }.runTaskTimer(plugin, 20L, 5L); // Start after 1 second, update every 5 ticks (4 times per second)
    }

    /**
     * Highly optimized VFX update system using spatial partitioning and caching
     */
    private void updateRespawnPointVFX() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastVFXUpdate < VFX_UPDATE_INTERVAL) {
            return;
        }
        lastVFXUpdate = currentTime;

        updatePlayerVFXCache();

        Set<RespawnPoint> previouslyActive = new HashSet<>(activeRespawnPoints);
        activeRespawnPoints.clear();

        processRespawnPoints(Bukkit.getWorlds().getFirst()); // Ignore not Erethon worlds

        for (RespawnPoint respawnPoint : previouslyActive) {
            if (!activeRespawnPoints.contains(respawnPoint)) {
                respawnPoint.hideAllDisplays();
            }
        }
    }

    /**
     * Updates the player VFX cache with current player locations
     */
    private void updatePlayerVFXCache() {
        playerVFXCache.clear();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playerVFXCache.put(player.getName(), new PlayerVFXData(player));
        }
    }

    private void processRespawnPoints(World world) {
        String worldName = world.getName();
        Set<RespawnPoint> worldRespawnPoints = respawnPointsByWorld.get(worldName);

        if (worldRespawnPoints == null || worldRespawnPoints.isEmpty()) {
            rebuildWorldCache(world);
            worldRespawnPoints = respawnPointsByWorld.get(worldName);
            if (worldRespawnPoints == null) return;
        }

        List<PlayerVFXData> worldPlayers = new ArrayList<>();
        for (PlayerVFXData playerData : playerVFXCache.values()) {
            if (worldName.equals(playerData.worldName)) {
                worldPlayers.add(playerData);
            }
        }

        if (worldPlayers.isEmpty()) {
            for (RespawnPoint respawnPoint : worldRespawnPoints) {
                respawnPoint.hideAllDisplays();
            }
            return;
        }

        for (RespawnPoint respawnPoint : worldRespawnPoints) {
            if (!shouldHaveVFX(respawnPoint)) {
                continue;
            }

            Location loc = respawnPoint.location();
            if (loc == null) continue;

            int chunkX = loc.getBlockX() >> 4;
            int chunkZ = loc.getBlockZ() >> 4;
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                respawnPoint.hideAllDisplays();
                continue;
            }

            boolean hasNearbyPlayers = false;
            for (PlayerVFXData playerData : worldPlayers) {
                // Quick distance check using chunk coordinates first
                int playerChunkX = playerData.chunkX;
                int playerChunkZ = playerData.chunkZ;

                // If player is more than VFX_CHUNK_RANGE chunks away, skip expensive distance calculation
                if (Math.abs(playerChunkX - chunkX) > VFX_CHUNK_RANGE ||
                    Math.abs(playerChunkZ - chunkZ) > VFX_CHUNK_RANGE) {
                    continue;
                }

                // Only do expensive distance calculation if chunks are close
                if (playerData.location.distanceSquared(loc) <= VFX_RANGE_SQUARED) {
                    hasNearbyPlayers = true;
                    break;
                }
            }

            if (hasNearbyPlayers) {
                activeRespawnPoints.add(respawnPoint);
                respawnPoint.updateVFX();
            } else {
                respawnPoint.hideAllDisplays();
            }
        }
    }

    /**
     * Rebuilds the world-based respawn point cache
     */
    private void rebuildWorldCache(World world) {
        String worldName = world.getName();
        Set<RespawnPoint> worldPoints = new HashSet<>();

        if (plugin.getRespawnPointManager() != null) {
            for (RespawnPoint point : plugin.getRespawnPointManager().getRespawnPoints()) {
                if (shouldHaveVFX(point) && point.location() != null &&
                    worldName.equals(point.location().getWorld().getName())) {
                    worldPoints.add(point);
                }
            }
        }

        respawnPointsByWorld.put(worldName, worldPoints);
    }

    /**
     * Rebuilds all world caches - call this when respawn points are added/removed
     */
    public void rebuildVFXCache() {
        respawnPointsByWorld.clear();
        for (World world : plugin.getServer().getWorlds()) {
            rebuildWorldCache(world);
        }
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
            rebuildVFXCache();
            startCentralVFX();
        }
    }

    /**
     * Stops the VFX system and cleans up all displays
     */
    public void stopVFX() {
        if (centralVfxTask != null) {
            centralVfxTask.cancel();
            centralVfxTask = null;
        }

        // Clean up all active displays
        for (RespawnPoint respawnPoint : activeRespawnPoints) {
            respawnPoint.cleanupAllDisplays();
        }

        activeRespawnPoints.clear();
        playerVFXCache.clear();
        respawnPointsByWorld.clear();
    }

    /**
     * Enables or disables VFX system
     */
    public void setVFXEnabled(boolean enabled) {
        if (this.vfxEnabled == enabled) return;

        this.vfxEnabled = enabled;
        if (enabled) {
            initializeVFX();
        } else {
            stopVFX();
        }
    }

    /**
     * Returns whether VFX is currently enabled
     */
    public boolean isVFXEnabled() {
        return vfxEnabled;
    }

    /**
     * Gets performance statistics for monitoring
     */
    public Map<String, Object> getVFXStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("vfxEnabled", vfxEnabled);
        stats.put("activeRespawnPoints", activeRespawnPoints.size());
        stats.put("cachedPlayers", playerVFXCache.size());
        stats.put("worldCaches", respawnPointsByWorld.size());
        stats.put("updateInterval", VFX_UPDATE_INTERVAL);
        stats.put("vfxRange", Math.sqrt(VFX_RANGE_SQUARED));
        stats.put("chunkRange", VFX_CHUNK_RANGE);

        int totalCachedPoints = 0;
        for (Set<RespawnPoint> worldPoints : respawnPointsByWorld.values()) {
            totalCachedPoints += worldPoints.size();
        }
        stats.put("totalCachedRespawnPoints", totalCachedPoints);

        return stats;
    }
}
