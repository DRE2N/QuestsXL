package de.erethon.questsxl.instancing;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central manager for the instancing system.
 * Handles template management, active instances, and player-instance associations.
 */
public class InstanceManager {

    private final QuestsXL plugin;

    /**
     * Registered templates by ID.
     */
    private final Map<String, InstanceTemplate> templates = new ConcurrentHashMap<>();

    /**
     * Active instances by their unique ID.
     */
    private final Map<String, InstancedArea> activeInstances = new ConcurrentHashMap<>();

    /**
     * Mapping of players to their active instance.
     */
    private final Map<QPlayer, InstancedArea> playerInstances = new ConcurrentHashMap<>();

    /**
     * Counter for generating unique instance IDs.
     */
    private final AtomicLong instanceCounter = new AtomicLong(0);

    /**
     * DAO for database persistence.
     */
    private InstanceDao instanceDao;

    /**
     * Player position selections for template creation.
     * Maps player UUID to their pos1 selection.
     */
    private final Map<UUID, Location> pos1Selections = new ConcurrentHashMap<>();

    /**
     * Player position selections for template creation.
     * Maps player UUID to their pos2 selection.
     */
    private final Map<UUID, Location> pos2Selections = new ConcurrentHashMap<>();

    private int autoSaveTaskId = -1;

    public InstanceManager(QuestsXL plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the instance manager with database access.
     */
    public void initialize() {
        this.instanceDao = plugin.getDatabaseManager().getInstanceDao();

        // Register inventory listener for syncing virtual container contents
        plugin.getServer().getPluginManager().registerEvents(new InstanceInventoryListener(plugin), plugin);

        // Load templates from database
        loadAllTemplatesFromDatabase();

        // Start auto-save task (runs every 30 seconds)
        startAutoSaveTask();

        QuestsXL.log("Instance manager initialized");
    }

    /**
     * Loads all templates from the database.
     */
    private void loadAllTemplatesFromDatabase() {
        if (instanceDao == null) {
            QuestsXL.log("[InstanceManager] No database connection, skipping template loading");
            return;
        }

        try {
            java.util.List<String> templateIds = instanceDao.getAllTemplateIds();
            if (templateIds == null || templateIds.isEmpty()) {
                QuestsXL.log("[InstanceManager] No templates found in database");
                return;
            }

            QuestsXL.log("[InstanceManager] Loading " + templateIds.size() + " templates from database...");

            for (String templateId : templateIds) {
                try {
                    InstanceTemplate template = InstancePersistence.loadTemplate(instanceDao, templateId).join();
                    if (template != null) {
                        templates.put(templateId, template);
                        QuestsXL.log("[InstanceManager] Loaded template: " + templateId);
                    } else {
                        QuestsXL.log("[InstanceManager] Failed to load template: " + templateId);
                    }
                } catch (Exception e) {
                    QuestsXL.log("[InstanceManager] Error loading template '" + templateId + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }

            QuestsXL.log("[InstanceManager] Loaded " + templates.size() + " templates");
        } catch (Exception e) {
            QuestsXL.log("[InstanceManager] Error loading templates from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== Template Management ====================

    /**
     * Sets pos1 selection for a player.
     */
    public void setPos1(Player player, Location location) {
        pos1Selections.put(player.getUniqueId(), location.clone());
    }

    /**
     * Sets pos2 selection for a player.
     */
    public void setPos2(Player player, Location location) {
        pos2Selections.put(player.getUniqueId(), location.clone());
    }

    /**
     * Gets pos1 selection for a player.
     */
    public Location getPos1(Player player) {
        return pos1Selections.get(player.getUniqueId());
    }

    /**
     * Gets pos2 selection for a player.
     */
    public Location getPos2(Player player) {
        return pos2Selections.get(player.getUniqueId());
    }

    /**
     * Clears position selections for a player.
     */
    public void clearSelections(Player player) {
        pos1Selections.remove(player.getUniqueId());
        pos2Selections.remove(player.getUniqueId());
    }

    /**
     * Creates a new template from a world region.
     *
     * @param id The template ID
     * @param world The world containing the region
     * @param bounds The bounding box defining the region
     * @return The created template
     */
    public InstanceTemplate createTemplate(String id, World world, BoundingBox bounds) {
        if (templates.containsKey(id)) {
            throw new IllegalArgumentException("Template with ID '" + id + "' already exists");
        }

        InstanceTemplate template = new InstanceTemplate(id, world, bounds);
        template.captureFromWorld();
        templates.put(id, template);

        QuestsXL.log("Created instance template '" + id + "' with " +
                     template.getBaseBlocks().size() + " blocks");

        // Auto-save template to database
        if (instanceDao != null) {
            saveTemplate(template).thenRun(() ->
                QuestsXL.log("Auto-saved template '" + id + "' to database")
            );
        }

        return template;
    }

    /**
     * Gets a template by ID.
     */
    public InstanceTemplate getTemplate(String id) {
        return templates.get(id);
    }

    /**
     * Gets all registered templates.
     */
    public Collection<InstanceTemplate> getTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }

    /**
     * Registers an externally created template.
     */
    public void registerTemplate(InstanceTemplate template) {
        templates.put(template.getId(), template);
    }

    /**
     * Removes a template. Active instances using this template will continue to work
     * but no new instances can be created.
     */
    public void removeTemplate(String id) {
        templates.remove(id);
    }

    /**
     * Saves a template to the database.
     */
    public CompletableFuture<Void> saveTemplate(InstanceTemplate template) {
        if (instanceDao == null) {
            return CompletableFuture.completedFuture(null);
        }
        return InstancePersistence.saveTemplate(instanceDao, template);
    }

    /**
     * Loads a template from the database.
     */
    public CompletableFuture<InstanceTemplate> loadTemplate(String id) {
        if (instanceDao == null) {
            return CompletableFuture.completedFuture(null);
        }
        return InstancePersistence.loadTemplate(instanceDao, id).thenApply(template -> {
            if (template != null) {
                templates.put(id, template);
            }
            return template;
        });
    }

    // ==================== Instance Management ====================

    /**
     * Enters a player into an instance for a template.
     * If the player's character already has a saved instance, it will be loaded.
     * Otherwise, a new instance is created.
     *
     * @param player The player to enter
     * @param templateId The template ID
     * @return The instance the player entered
     */
    public InstancedArea enterInstance(QPlayer player, String templateId) {
        return enterInstance(Set.of(player), templateId);
    }

    /**
     * Enters multiple players into a shared instance.
     * Uses the first player's character ID as the owner for persistence.
     * If the player has a saved instance for this template, it will be loaded.
     *
     * @param players The players to enter
     * @param templateId The template ID
     * @return The instance the players entered
     */
    public InstancedArea enterInstance(Set<QPlayer> players, String templateId) {
        if (players.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one player");
        }

        InstanceTemplate template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template '" + templateId + "' not found");
        }

        // Remove players from any current instances
        for (QPlayer player : players) {
            leaveInstance(player);
        }

        // Try to find owner's existing instance or create new one
        QPlayer owner = players.iterator().next();
        UUID characterId = getCharacterId(owner);

        // Try to load existing instance state from database
        InstancedArea instance = null;
        if (instanceDao != null && characterId != null) {
            try {
                instance = InstancePersistence.loadInstanceState(instanceDao, characterId, templateId, template).join();
                if (instance != null) {
                    QuestsXL.log("[InstanceManager] Loaded existing instance state for character " + characterId);
                }
            } catch (Exception e) {
                QuestsXL.log("[InstanceManager] Failed to load instance state: " + e.getMessage());
            }
        }

        // Create new instance if none was loaded
        if (instance == null) {
            String instanceId = templateId + "_" + instanceCounter.incrementAndGet();
            instance = new InstancedArea(instanceId, template, characterId);
            QuestsXL.log("[InstanceManager] Created new instance for character " + characterId);
        }

        // Add all players as participants
        for (QPlayer player : players) {
            instance.addParticipant(player);
            playerInstances.put(player, instance);
            QuestsXL.log("[InstanceManager] Added player " + player.getPlayer().getName() +
                        " to playerInstances map. Total tracked: " + playerInstances.size());
        }

        activeInstances.put(instance.getId(), instance);

        QuestsXL.log("[InstanceManager] Instance bounds: " + template.getBounds());
        QuestsXL.log("[InstanceManager] Instance has " + instance.getModifiedBlocks().size() + " modified blocks");

        // Update player visibility for all players entering this instance
        for (QPlayer player : players) {
            updatePlayerVisibility(player, instance);
        }

        // Send initial chunk data to players
        for (QPlayer player : players) {
            sendInstanceChunks(player, instance);
        }

        QuestsXL.log("Players entered instance '" + instance.getId() + "' (template: " + templateId + ")");

        return instance;
    }

    /**
     * Removes a player from their current instance.
     *
     * @param player The player to remove
     */
    public void leaveInstance(QPlayer player) {
        InstancedArea instance = playerInstances.remove(player);
        if (instance == null) {
            return;
        }

        instance.removeParticipant(player);

        resetPlayerVisibility(player);

        resetPlayerView(player, instance);

        if (instance.isEmpty()) {
            handleEmptyInstance(instance);
        }
    }

    /**
     * Gets a player's active instance.
     */
    public InstancedArea getActiveInstance(QPlayer player) {
        return playerInstances.get(player);
    }

    /**
     * Gets an active instance by ID.
     */
    public InstancedArea getInstance(String instanceId) {
        return activeInstances.get(instanceId);
    }

    /**
     * Gets all active instances.
     */
    public Collection<InstancedArea> getActiveInstances() {
        return Collections.unmodifiableCollection(activeInstances.values());
    }

    /**
     * Checks if a player is in any instance.
     */
    public boolean isInInstance(QPlayer player) {
        return playerInstances.containsKey(player);
    }

    /**
     * Gets the instance DAO.
     */
    public InstanceDao getInstanceDao() {
        return instanceDao;
    }

    /**
     * Sets a block within an instance.
     *
     * @param instance The instance to modify
     * @param pos The block position
     * @param state The new block state
     */
    public void setBlock(InstancedArea instance, BlockPos pos, BlockState state) {
        instance.setBlock(pos, state);

        // Send block update to all participants
        for (QPlayer player : instance.getParticipants()) {
            sendBlockUpdate(player, pos, state);
        }
    }

    /**
     * Gets the effective block state at a position in an instance.
     */
    public BlockState getBlock(InstancedArea instance, BlockPos pos) {
        return instance.getEffectiveBlockState(pos);
    }

    /**
     * Resets an instance to its template state.
     */
    public void resetInstance(InstancedArea instance) {
        instance.reset();

        // Resend all chunks to participants
        for (QPlayer player : instance.getParticipants()) {
            sendInstanceChunks(player, instance);
        }

        QuestsXL.log("Reset instance '" + instance.getId() + "'");
    }

    // ==================== Persistence ====================

    /**
     * Saves an instance's state to the database.
     */
    public CompletableFuture<Void> saveInstance(InstancedArea instance) {
        if (instanceDao == null || instance.getOwnerCharacterId() == null) {
            return CompletableFuture.completedFuture(null);
        }

        return InstancePersistence.saveInstanceState(instanceDao, instance)
                .thenAccept(v -> instance.setDirty(false)); // Only mark not dirty after save completes
    }

    /**
     * Loads an instance's state from the database.
     */
    public CompletableFuture<InstancedArea> loadInstance(UUID characterId, String templateId) {
        if (instanceDao == null) {
            return CompletableFuture.completedFuture(null);
        }

        InstanceTemplate template = templates.get(templateId);
        if (template == null) {
            return CompletableFuture.completedFuture(null);
        }

        return InstancePersistence.loadInstanceState(instanceDao, characterId, templateId, template);
    }

    /**
     * Saves all dirty instances.
     */
    public CompletableFuture<Void> saveAllDirtyInstances() {
        return CompletableFuture.allOf(
                activeInstances.values().stream()
                        .filter(InstancedArea::isDirty)
                        .map(this::saveInstance)
                        .toArray(CompletableFuture[]::new)
        );
    }

    // ==================== Internal Methods ====================

    /**
     * Handles cleanup when an instance becomes empty.
     */
    private void handleEmptyInstance(InstancedArea instance) {
        // Always sync and save block entities when unloading
        // (player may have modified chest contents)
        if (instance.getOwnerCharacterId() != null) {
            QuestsXL.log("[InstanceManager] Saving instance '" + instance.getId() + "' before unload");
            saveInstance(instance).join(); // Wait for save to complete
        }

        // Remove from active instances
        activeInstances.remove(instance.getId());

        QuestsXL.log("Unloaded empty instance '" + instance.getId() + "'");
    }

    /**
     * Sends instance chunk data to a player.
     */
    private void sendInstanceChunks(QPlayer player, InstancedArea instance) {
        // Force chunk reload for chunks within instance bounds
        BoundingBox bounds = instance.getTemplate().getBounds();
        World world = instance.getTemplate().getWorld();

        if (world == null || !world.equals(player.getPlayer().getWorld())) {
            return;
        }

        int minChunkX = (int) Math.floor(bounds.getMinX()) >> 4;
        int maxChunkX = (int) Math.floor(bounds.getMaxX()) >> 4;
        int minChunkZ = (int) Math.floor(bounds.getMinZ()) >> 4;
        int maxChunkZ = (int) Math.floor(bounds.getMaxZ()) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                // Refresh chunks by sending forget + reload packets
                InstancePacketHelper.refreshChunk(player.getPlayer(), world, cx, cz);
            }
        }
    }

    /**
     * Resets a player's view to show the real world blocks.
     */
    private void resetPlayerView(QPlayer player, InstancedArea instance) {
        // Refresh chunks to show actual world state
        sendInstanceChunks(player, instance);
    }

    /**
     * Sends a block update to a player.
     */
    private void sendBlockUpdate(QPlayer player, BlockPos pos, BlockState state) {
        InstancePacketHelper.sendBlockUpdate(player.getPlayer(), pos, state);
    }

    /**
     * Gets the character ID for a player.
     */
    private UUID getCharacterId(QPlayer player) {
        return plugin.getDatabaseManager().getCurrentCharacterId(player.getPlayer());
    }

    /**
     * Updates player visibility when entering an instance.
     * Hides players who are in different instances or not in any instance.
     * Shows players who are in the same instance.
     */
    private void updatePlayerVisibility(QPlayer enteringPlayer, InstancedArea instance) {
        Player player = enteringPlayer.getPlayer();
        World world = instance.getTemplate().getWorld();

        if (world == null) {
            return;
        }

        // Check all players in the same world
        for (Player otherPlayer : world.getPlayers()) {
            if (otherPlayer.equals(player)) {
                continue; // Skip self
            }

            QPlayer otherQPlayer = plugin.getDatabaseManager().getCurrentPlayer(otherPlayer);
            if (otherQPlayer == null) {
                continue;
            }

            InstancedArea otherInstance = playerInstances.get(otherQPlayer);

            if (otherInstance != null && otherInstance.equals(instance)) {
                // Same instance - make sure they can see each other
                player.showEntity(plugin, otherPlayer);
                otherPlayer.showEntity(plugin, player);
                QuestsXL.log("[InstanceManager] Players " + player.getName() + " and " + otherPlayer.getName() +
                           " can see each other (same instance)");
            } else {
                // Different instance or no instance - hide them from each other
                player.hideEntity(plugin, otherPlayer);
                otherPlayer.hideEntity(plugin, player);
                QuestsXL.log("[InstanceManager] Players " + player.getName() + " and " + otherPlayer.getName() +
                           " hidden from each other (different instances)");
            }
        }
    }

    /**
     * Resets player visibility when leaving an instance.
     * Shows all players again that were previously hidden.
     */
    private void resetPlayerVisibility(QPlayer leavingPlayer) {
        Player player = leavingPlayer.getPlayer();
        World world = player.getWorld();

        // Show all players in the same world again
        for (Player otherPlayer : world.getPlayers()) {
            if (otherPlayer.equals(player)) {
                continue; // Skip self
            }

            // Show both ways
            player.showEntity(plugin, otherPlayer);
            otherPlayer.showEntity(plugin, player);
        }

        QuestsXL.log("[InstanceManager] Reset visibility for player " + player.getName());
    }

    /**
     * Shutdown cleanup - save all instances.
     */
    public void shutdown() {
        // Cancel auto-save task
        if (autoSaveTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;
        }

        QuestsXL.log("Saving all instances...");
        saveAllDirtyInstances().join();
        activeInstances.clear();
        playerInstances.clear();
    }

    /**
     * Starts the auto-save task that periodically saves dirty instances.
     */
    private void startAutoSaveTask() {
        autoSaveTaskId = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            int dirtyCount = 0;
            int savedCount = 0;

            for (InstancedArea instance : activeInstances.values()) {
                if (instance.isDirty()) {
                    dirtyCount++;
                    saveInstance(instance).thenRun(() -> {
                        // Yay
                    }).exceptionally(e -> {
                        QuestsXL.log("[InstanceManager] Auto-save failed for instance '" + instance.getId() + "': " + e.getMessage());
                        return null;
                    });
                    savedCount++;
                }
            }

            if (savedCount > 0) {
                QuestsXL.log("[InstanceManager] Auto-save: saving " + savedCount + " dirty instance(s)");
            }
        }, 600L, 600L).getTaskId();

        QuestsXL.log("[InstanceManager] Auto-save task started (every 30 seconds)");
    }
}

