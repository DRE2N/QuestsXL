package de.erethon.questsxl.interaction;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all world interactions and handles proximity-based activation.
 * Uses chunk-based spatial indexing for efficient proximity checks.
 */
public class InteractionManager {

    private final QuestsXL plugin;

    // All interactions by ID
    private final Map<String, WorldInteraction> interactions = new ConcurrentHashMap<>();

    // Chunk-based spatial index: ChunkKey -> Set of interactions
    private final Map<ChunkKey, Set<WorldInteraction>> spatialIndex = new ConcurrentHashMap<>();

    // Update task for proximity checking
    private BukkitTask updateTask;

    // How often to check proximity (in ticks)
    private static final long UPDATE_INTERVAL = 40; // 2 seconds

    public InteractionManager(QuestsXL plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads all interactions from the interactions directory
     */
    public void loadInteractions(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
            QuestsXL.log("Created interactions directory: " + directory.getAbsolutePath());
            return;
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            QuestsXL.log("No interaction files found in " + directory.getAbsolutePath());
            return;
        }

        int loaded = 0;
        for (File file : files) {
            try {
                loadInteractionFile(file);
                loaded++;
            } catch (Exception e) {
                QuestsXL.log("Failed to load interaction file " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        QuestsXL.log("Loaded " + loaded + " interaction files with " + interactions.size() + " total interactions");
        startUpdateTask();
    }

    /**
     * Loads interactions from a single file
     */
    private void loadInteractionFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            try {
                WorldInteraction interaction = new WorldInteraction(key, section);
                addInteraction(interaction);
            } catch (Exception e) {
                QuestsXL.log("Failed to load interaction '" + key + "' from " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Adds an interaction to the manager and spatial index
     */
    public void addInteraction(WorldInteraction interaction) {
        interactions.put(interaction.getId(), interaction);
        indexInteraction(interaction);
    }

    /**
     * Indexes an interaction by its chunk location(s)
     */
    private void indexInteraction(WorldInteraction interaction) {
        Location loc = interaction.getLocation();
        double radius = interaction.getActivationRadius();

        // Calculate which chunks this interaction could affect
        // We need to index it in all chunks within radius
        int minChunkX = (int) Math.floor((loc.getX() - radius) / 16);
        int maxChunkX = (int) Math.floor((loc.getX() + radius) / 16);
        int minChunkZ = (int) Math.floor((loc.getZ() - radius) / 16);
        int maxChunkZ = (int) Math.floor((loc.getZ() + radius) / 16);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkKey key = new ChunkKey(loc.getWorld().getName(), chunkX, chunkZ);
                spatialIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(interaction);
            }
        }
    }

    /**
     * Removes an interaction from the manager
     */
    public void removeInteraction(String id) {
        WorldInteraction interaction = interactions.remove(id);
        if (interaction != null) {
            interaction.deactivate();
            // Remove from spatial index
            spatialIndex.values().forEach(set -> set.remove(interaction));
        }
    }

    /**
     * Gets an interaction by ID
     */
    public WorldInteraction getInteraction(String id) {
        return interactions.get(id);
    }

    /**
     * Gets all interactions
     */
    public Collection<WorldInteraction> getAllInteractions() {
        return interactions.values();
    }

    /**
     * Gets interactions near a location
     */
    public Set<WorldInteraction> getInteractionsNear(Location location) {
        ChunkKey key = new ChunkKey(location.getWorld().getName(),
                                   location.getBlockX() >> 4,
                                   location.getBlockZ() >> 4);
        return spatialIndex.getOrDefault(key, Collections.emptySet());
    }

    /**
     * Starts the periodic update task that checks player proximity
     */
    private void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateProximity();
            }
        }.runTaskTimer(plugin, UPDATE_INTERVAL, UPDATE_INTERVAL);
    }

    /**
     * Updates proximity for all players and interactions
     */
    private void updateProximity() {
        // Get all online QPlayers
        Collection<QPlayer> onlinePlayers = plugin.getDatabaseManager().getPlayers();

        for (QPlayer player : onlinePlayers) {
            Location playerLoc = player.getLocation();
            if (playerLoc == null) {
                continue;
            }

            // Get interactions near this player's chunk
            Set<WorldInteraction> nearbyInteractions = getInteractionsNear(playerLoc);

            for (WorldInteraction interaction : nearbyInteractions) {
                boolean inRange = interaction.isPlayerInRange(player);
                boolean wasInRange = interaction.getPlayersInRange().contains(player);

                if (inRange && !wasInRange) {
                    // Player entered range
                    interaction.onPlayerEnter(player);
                } else if (!inRange && wasInRange) {
                    // Player left range
                    interaction.onPlayerLeave(player);
                }
            }

            // Also check if player left range of any interactions they were in
            for (WorldInteraction interaction : interactions.values()) {
                if (interaction.getPlayersInRange().contains(player) && !interaction.isPlayerInRange(player)) {
                    interaction.onPlayerLeave(player);
                }
            }
        }
    }

    /**
     * Cleans up when a player leaves the server
     */
    public void onPlayerLeave(QPlayer player) {
        for (WorldInteraction interaction : interactions.values()) {
            if (interaction.getPlayersInRange().contains(player)) {
                interaction.onPlayerLeave(player);
            }
        }
    }

    /**
     * Shuts down the manager
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Deactivate all interactions
        for (WorldInteraction interaction : interactions.values()) {
            interaction.deactivate();
        }

        interactions.clear();
        spatialIndex.clear();
    }

    /**
     * Reloads all interactions
     */
    public void reload(File directory) {
        shutdown();
        loadInteractions(directory);
    }

    /**
     * Simple chunk key for spatial indexing
     */
    private static class ChunkKey {
        private final String world;
        private final int x;
        private final int z;

        public ChunkKey(String world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkKey chunkKey = (ChunkKey) o;
            return x == chunkKey.x && z == chunkKey.z && world.equals(chunkKey.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, z);
        }

        @Override
        public String toString() {
            return world + ":" + x + "," + z;
        }
    }
}

