package de.erethon.questsxl.region;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.instancing.InstanceManager;
import de.erethon.questsxl.instancing.InstanceTemplate;
import de.erethon.questsxl.instancing.InstancedArea;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.BoundingBox;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that automatically handles player entry/exit for instanced regions.
 * Similar to ApartmentService but for quest regions.
 */
public class RegionInstanceService implements Listener {

    private final QuestsXL plugin;
    private final InstanceManager instanceManager;
    private final QRegionManager regionManager;

    /**
     * Tracks which instanced region template each player is currently in.
     * Maps player UUID to region instance template ID.
     */
    private final Map<UUID, String> currentRegionInstance = new ConcurrentHashMap<>();

    public RegionInstanceService(QuestsXL plugin, InstanceManager instanceManager, QRegionManager regionManager) {
        this.plugin = plugin;
        this.instanceManager = instanceManager;
        this.regionManager = regionManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Creates an instance template for a region.
     * This is called when a region is set to instanced.
     */
    public void createTemplateForRegion(QRegion region) {
        if (region.getPos1() == null || region.getPos2() == null) {
            QuestsXL.log("[RegionInstanceService] Cannot create template for region " + region.getId() + " - positions not set");
            return;
        }

        String templateId = region.getInstanceTemplateId();

        // Check if template already exists
        if (instanceManager.getTemplate(templateId) != null) {
            QuestsXL.log("[RegionInstanceService] Template " + templateId + " already exists");
            return;
        }

        World world = region.getPos1().getWorld();
        if (world == null) {
            QuestsXL.log("[RegionInstanceService] Cannot create template for region " + region.getId() + " - world is null");
            return;
        }

        BoundingBox bounds = region.getBoundingBox();
        if (bounds == null) {
            QuestsXL.log("[RegionInstanceService] Cannot create template for region " + region.getId() + " - bounding box is null");
            return;
        }

        try {
            InstanceTemplate template = instanceManager.createTemplate(templateId, world, bounds);
            QuestsXL.log("[RegionInstanceService] Created instance template for region " + region.getId() +
                        " with " + template.getBaseBlocks().size() + " blocks");
        } catch (Exception e) {
            QuestsXL.log("[RegionInstanceService] Error creating template for region " + region.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles automatic instance entry/exit based on player movement.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        Player player = event.getPlayer();
        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        if (qPlayer == null) {
            return; // Player likely in character selection
        }

        Location to = event.getTo();
        Location from = event.getFrom();

        // Check all instanced regions to see if player should enter/exit
        for (QRegion region : regionManager.getRegions()) {
            if (!region.isInstanced()) {
                continue;
            }

            boolean wasInRegion = region.isInRegion(from);
            boolean isInRegion = region.isInRegion(to);
            boolean wasNearRegion = isNearRegion(region, from);
            boolean isNearRegion = isNearRegion(region, to);

            String templateId = region.getInstanceTemplateId();
            String currentTemplate = currentRegionInstance.get(player.getUniqueId());

            // Player entering region or proximity
            if (!wasInRegion && !wasNearRegion && (isInRegion || isNearRegion)) {
                enterRegionInstance(qPlayer, region);
            }
            // Player leaving region and proximity
            else if ((wasInRegion || wasNearRegion) && !isInRegion && !isNearRegion && templateId.equals(currentTemplate)) {
                leaveRegionInstance(qPlayer, region);
            }
        }
    }

    /**
     * Checks if a location is near a region based on its proximity distance.
     */
    private boolean isNearRegion(QRegion region, Location location) {
        if (region.getProximityDistance() <= 0) {
            return false; // Proximity disabled
        }

        if (region.getPos1() == null || region.getPos2() == null) {
            return false;
        }

        if (!location.getWorld().equals(region.getPos1().getWorld())) {
            return false;
        }

        // Check if within proximity distance (in chunks)
        int proximityBlocks = region.getProximityDistance() * 16;

        double minX = Math.min(region.getPos1().getX(), region.getPos2().getX());
        double maxX = Math.max(region.getPos1().getX(), region.getPos2().getX());
        double minY = Math.min(region.getPos1().getY(), region.getPos2().getY());
        double maxY = Math.max(region.getPos1().getY(), region.getPos2().getY());
        double minZ = Math.min(region.getPos1().getZ(), region.getPos2().getZ());
        double maxZ = Math.max(region.getPos1().getZ(), region.getPos2().getZ());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        // Expand bounds by proximity distance
        return x >= minX - proximityBlocks && x <= maxX + proximityBlocks &&
               y >= minY - proximityBlocks && y <= maxY + proximityBlocks &&
               z >= minZ - proximityBlocks && z <= maxZ + proximityBlocks;
    }

    /**
     * Enters a player into a region instance.
     */
    private void enterRegionInstance(QPlayer qPlayer, QRegion region) {
        String templateId = region.getInstanceTemplateId();

        // Check if template exists
        if (instanceManager.getTemplate(templateId) == null) {
            QuestsXL.log("[RegionInstanceService] Template " + templateId + " not found, creating it now");
            createTemplateForRegion(region);
        }

        // Don't enter if already in this instance
        if (instanceManager.isInInstance(qPlayer)) {
            InstancedArea current = instanceManager.getActiveInstance(qPlayer);
            if (current != null && current.getTemplate().getId().equals(templateId)) {
                return; // Already in this instance
            }
            // Leave current instance before entering new one
            instanceManager.leaveInstance(qPlayer);
        }

        try {
            QuestsXL.log("[RegionInstanceService] Player " + qPlayer.getPlayer().getName() +
                        " entering instance for region " + region.getId());
            instanceManager.enterInstance(qPlayer, templateId);
            currentRegionInstance.put(qPlayer.getPlayer().getUniqueId(), templateId);
        } catch (Exception e) {
            QuestsXL.log("[RegionInstanceService] Error entering instance for region " + region.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Removes a player from a region instance.
     */
    private void leaveRegionInstance(QPlayer qPlayer, QRegion region) {
        String templateId = region.getInstanceTemplateId();

        if (!instanceManager.isInInstance(qPlayer)) {
            return;
        }

        InstancedArea current = instanceManager.getActiveInstance(qPlayer);
        if (current == null || !current.getTemplate().getId().equals(templateId)) {
            return; // Not in this region's instance
        }

        try {
            QuestsXL.log("[RegionInstanceService] Player " + qPlayer.getPlayer().getName() +
                        " leaving instance for region " + region.getId());
            instanceManager.leaveInstance(qPlayer);
            currentRegionInstance.remove(qPlayer.getPlayer().getUniqueId());
        } catch (Exception e) {
            QuestsXL.log("[RegionInstanceService] Error leaving instance for region " + region.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Forces a player to leave any region instance they're currently in.
     * Useful for cleanup when players disconnect.
     */
    public void forceLeaveInstance(QPlayer qPlayer) {
        if (currentRegionInstance.containsKey(qPlayer.getPlayer().getUniqueId())) {
            instanceManager.leaveInstance(qPlayer);
            currentRegionInstance.remove(qPlayer.getPlayer().getUniqueId());
        }
    }
}

