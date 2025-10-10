package de.erethon.questsxl.interaction;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.Completable;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A world interaction is a location-based objective holder that activates when players are nearby.
 * Unlike GlobalObjectives which are added to every player, WorldInteractions only register their
 * objectives when at least one player is in range.
 *
 * Example use cases:
 * - Levers that activate cranes
 * - Buttons that open doors
 * - Pressure plates that trigger traps
 * - Interactive objects in the world
 */
public class WorldInteraction implements ObjectiveHolder, QComponent {

    private final QuestsXL plugin = QuestsXL.get();

    private String id;
    private Location location;
    private double activationRadius = 32.0; // Default radius

    private final Set<QObjective> objectives = new HashSet<>();
    private final Set<ActiveObjective> activeObjectives = ConcurrentHashMap.newKeySet();
    private final Set<QPlayer> playersInRange = ConcurrentHashMap.newKeySet();

    private boolean isActive = false;
    private QComponent parent;

    public WorldInteraction(String id, Location location, double activationRadius) {
        this.id = id;
        this.location = location;
        this.activationRadius = activationRadius;
    }

    /**
     * Constructor for loading from config
     */
    public WorldInteraction(String id, ConfigurationSection section) {
        this.id = id;
        load(section);
    }

    private void load(ConfigurationSection section) {
        // Load location
        String worldName = section.getString("world");
        if (worldName == null) {
            throw new IllegalArgumentException("World must be specified for interaction " + id);
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalArgumentException("World " + worldName + " not found for interaction " + id);
        }

        double x = section.getDouble("x", 0);
        double y = section.getDouble("y", 64);
        double z = section.getDouble("z", 0);
        this.location = new Location(world, x, y, z);

        // Load activation radius
        this.activationRadius = section.getDouble("radius", 32.0);

        // Load objectives
        objectives.addAll((Collection<? extends QObjective>) QConfigLoader.load(
                this, "objectives", section, QRegistries.OBJECTIVES));

        QuestsXL.log("Loaded interaction '" + id + "' at " + location + " with " + objectives.size() + " objectives");
    }

    /**
     * Activates this interaction, registering all objectives with the event manager.
     * This should be called when the first player enters range.
     */
    public void activate() {
        if (isActive) {
            return;
        }

        isActive = true;

        // Create active objectives for this interaction
        for (QObjective objective : objectives) {
            ActiveObjective active = new ActiveObjective(this, null, null, objective);
            activeObjectives.add(active);
            plugin.getObjectiveEventManager().register(active);
        }

        QuestsXL.log("Activated interaction: " + id);
    }

    /**
     * Deactivates this interaction, unregistering all objectives.
     * This should be called when the last player leaves range.
     */
    public void deactivate() {
        if (!isActive) {
            return;
        }

        isActive = false;

        // Unregister and clear all active objectives
        for (ActiveObjective active : activeObjectives) {
            plugin.getObjectiveEventManager().unregister(active);
        }
        activeObjectives.clear();

        QuestsXL.log("Deactivated interaction: " + id);
    }

    /**
     * Called when a player enters the activation radius
     */
    public void onPlayerEnter(QPlayer player) {
        boolean wasEmpty = playersInRange.isEmpty();
        playersInRange.add(player);

        // Activate if this is the first player
        if (wasEmpty) {
            activate();
        }
    }

    /**
     * Called when a player leaves the activation radius
     */
    public void onPlayerLeave(QPlayer player) {
        playersInRange.remove(player);

        // Deactivate if no players remain
        if (playersInRange.isEmpty()) {
            deactivate();
        }
    }

    /**
     * Checks if a player is within activation radius
     */
    public boolean isPlayerInRange(QPlayer player) {
        Location playerLoc = player.getLocation();
        if (!playerLoc.getWorld().equals(location.getWorld())) {
            return false;
        }
        return playerLoc.distanceSquared(location) <= activationRadius * activationRadius;
    }

    // Getters

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public double getActivationRadius() {
        return activationRadius;
    }

    public Set<QPlayer> getPlayersInRange() {
        return playersInRange;
    }

    public boolean isActive() {
        return isActive;
    }

    public Set<QObjective> getObjectives() {
        return objectives;
    }

    // ObjectiveHolder implementation

    @Override
    public void addObjective(@NotNull ActiveObjective objective) {
        activeObjectives.add(objective);
    }

    @Override
    public boolean hasObjective(@NotNull QObjective objective) {
        return objectives.contains(objective);
    }

    @Override
    public Set<ActiveObjective> getCurrentObjectives() {
        return activeObjectives;
    }

    @Override
    public void removeObjective(@NotNull ActiveObjective objective) {
        activeObjectives.remove(objective);
    }

    @Override
    public void clearObjectives() {
        for (ActiveObjective active : activeObjectives) {
            plugin.getObjectiveEventManager().unregister(active);
        }
        activeObjectives.clear();
    }

    @Override
    public void progress(@NotNull Completable completable) {
        // WorldInteractions don't have stages or progression
        // Objectives complete and can be repeated
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public String getUniqueId() {
        return id;
    }


    @Override
    public QComponent getParent() {
        return parent;
    }

    @Override
    public void setParent(QComponent parent) {
        this.parent = parent;
    }

    @Override
    public String id() {
        return id;
    }
}

