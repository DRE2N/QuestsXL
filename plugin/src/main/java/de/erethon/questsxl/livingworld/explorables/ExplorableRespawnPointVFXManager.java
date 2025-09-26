package de.erethon.questsxl.livingworld.explorables;

import de.erethon.questsxl.QuestsXL;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized manager for all explorable respawn point visual effects.
 */
public class ExplorableRespawnPointVFXManager {

    private static ExplorableRespawnPointVFXManager instance;
    private final QuestsXL plugin = QuestsXL.get();
    private final Set<ExplorableRespawnPoint> registeredPoints = ConcurrentHashMap.newKeySet();
    private BukkitTask vfxTask;
    private final long startTime = System.currentTimeMillis();

    private ExplorableRespawnPointVFXManager() {
        startVFXTask();
    }

    public static ExplorableRespawnPointVFXManager getInstance() {
        if (instance == null) {
            instance = new ExplorableRespawnPointVFXManager();
        }
        return instance;
    }

    /**
     * Registers a respawn point for visual effects updates
     */
    public void register(ExplorableRespawnPoint point) {
        registeredPoints.add(point);
    }

    /**
     * Unregisters a respawn point from visual effects updates
     */
    public void unregister(ExplorableRespawnPoint point) {
        registeredPoints.remove(point);
        point.cleanupVFX(); // Clean up any existing displays
    }

    /**
     * Gets the time when the VFX system started (for animations)
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Starts the shared VFX update task
     */
    private void startVFXTask() {
        if (vfxTask != null) {
            vfxTask.cancel();
        }

        vfxTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllVFX();
            }
        }.runTaskTimer(plugin, 0L, 2L); // Update every 2 ticks (0.1 seconds)
    }

    /**
     * Updates visual effects for all registered respawn points
     */
    private void updateAllVFX() {
        // Create a copy to avoid concurrent modification during iteration
        Set<ExplorableRespawnPoint> pointsCopy = Set.copyOf(registeredPoints);

        for (ExplorableRespawnPoint point : pointsCopy) {
            try {
                point.updateVFX();
            } catch (Exception e) {
                // Log error but continue with other points
                plugin.getLogger().warning("Error updating VFX for respawn point " + point.id() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Shuts down the VFX manager and cleans up all resources
     */
    public void shutdown() {
        if (vfxTask != null) {
            vfxTask.cancel();
            vfxTask = null;
        }

        // Clean up all registered points
        for (ExplorableRespawnPoint point : registeredPoints) {
            point.cleanupVFX();
        }
        registeredPoints.clear();

        instance = null;
    }
}
