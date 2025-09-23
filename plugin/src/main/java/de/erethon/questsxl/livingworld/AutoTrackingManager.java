package de.erethon.questsxl.livingworld;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

/**
 * Manages automatic tracking of events and quests based on proximity and availability
 */
public class AutoTrackingManager {

    private final QuestsXL plugin;
    private static final int AUTO_TRACKING_PRIORITY = 1; // Lower priority than manual tracking
    private static final int UPDATE_INTERVAL = 40; // 2 seconds (40 ticks)

    public AutoTrackingManager(QuestsXL plugin) {
        this.plugin = plugin;
        startAutoTrackingTask();
    }

    /**
     * Starts the periodic task that handles automatic tracking
     */
    private void startAutoTrackingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAutoTracking();
            }
        }.runTaskTimer(plugin, UPDATE_INTERVAL, UPDATE_INTERVAL);
    }

    /**
     * Updates automatic tracking for all online players
     */
    private void updateAutoTracking() {
        if (plugin.getDatabaseManager() == null) return;

        for (QPlayer qPlayer : plugin.getDatabaseManager().getPlayers()) {
            if (!qPlayer.getPlayer().isOnline()) {
                continue;
            }

            updatePlayerAutoTracking(qPlayer);
        }
    }

    /**
     * Updates automatic tracking for a specific player
     */
    public void updatePlayerAutoTracking(QPlayer qPlayer) {
        // First check if currently tracked event is still active
        if (qPlayer.getTrackedEvent() != null && qPlayer.getTrackedEvent().getState() != EventState.ACTIVE) {
            qPlayer.setTrackedEvent(null, 0);
        }

        autoTrackNearestEvent(qPlayer);
    }

    /**
     * Automatically tracks the nearest active event within range
     */
    private void autoTrackNearestEvent(QPlayer qPlayer) {
        Location playerLocation = qPlayer.getPlayer().getLocation();
        QEvent nearestEvent = findNearestActiveEvent(playerLocation);

        if (nearestEvent != null) {
            // Only auto-track if current priority is low enough or no event is tracked
            if (qPlayer.getTrackedEvent() == null ||
                qPlayer.getCurrentTrackedEventPriority() <= AUTO_TRACKING_PRIORITY) {
                qPlayer.setTrackedEvent(nearestEvent, AUTO_TRACKING_PRIORITY);
            }
        } else {
            // Clear auto-tracked event if no events in range and current tracking is auto
            if (qPlayer.getTrackedEvent() != null &&
                qPlayer.getCurrentTrackedEventPriority() <= AUTO_TRACKING_PRIORITY) {
                qPlayer.setTrackedEvent(null, 0);
            }
        }
    }


    /**
     * Finds the nearest active event within range of the player
     */
    private QEvent findNearestActiveEvent(Location playerLocation) {
        Set<QEvent> activeEvents = plugin.getEventManager().getActiveEvents();
        QEvent nearestEvent = null;
        double nearestDistance = Double.MAX_VALUE;

        for (QEvent event : activeEvents) {
            Location eventLocation = event.getCenterLocation();
            if (eventLocation == null || !eventLocation.getWorld().equals(playerLocation.getWorld())) {
                continue;
            }

            double distance = playerLocation.distance(eventLocation);
            int eventRange = event.getRange();

            // Check if event is within range and closer than previously found events
            if (distance <= eventRange && distance < nearestDistance) {
                nearestEvent = event;
                nearestDistance = distance;
            }
        }

        return nearestEvent;
    }

    /**
     * Triggers immediate auto-tracking update for a player (called on movement/region change)
     */
    public void triggerImmediateUpdate(QPlayer qPlayer) {
        updatePlayerAutoTracking(qPlayer);
    }
}
