package de.erethon.questsxl.livingworld;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.explorables.LootChest;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A set of Explorables that players can discover.
 * For example, this could represent all Explorables within a Factions region.
 * Exploration sets can be nested. E.g., there could be one for the entirety of Azuria, and then one for each region.
 */
public final class ExplorationSet {

    private final String id;
    private ExplorationSet parent;
    private Set<ExplorationSet> children = new HashSet<>();
    private QTranslatable displayName;
    private QTranslatable description;
    private final List<Explorable> entries;
    private Location averageLocation;

    private final Map<Integer, List<QAction>> rewardActions = new HashMap<>();

    public ExplorationSet(String id) {
        this.id = id;
        this.parent = null;
        this.displayName = new QTranslatable("qxl.explorationset." + id + ".name", new HashMap<>());
        this.description = new QTranslatable("qxl.explorationset." + id + ".description", new HashMap<>());
        this.entries = new ArrayList<>();
    }

    public ExplorationSet(String id, ExplorationSet parent, QTranslatable displayName, QTranslatable description, List<Explorable> entries) {
        this.id = id;
        this.parent = parent;
        this.displayName = displayName;
        this.description = description;
        this.entries = entries;
    }

    public String id() {
        return id;
    }

    public QTranslatable displayName() {
        return displayName;
    }

    public QTranslatable description() {
        return description;
    }

    public void setDisplayName(QTranslatable displayName) {
        this.displayName = displayName;
    }

    public void setDescription(QTranslatable description) {
        this.description = description;
    }

    public List<Explorable> entries() {
        return entries;
    }

    public void setParent(ExplorationSet parent) {
        this.parent = parent;
        parent.children.add(this);
        recalculateAverageLocation();
    }

    public Location averageLocation() {
        if (averageLocation == null && !entries.isEmpty()) {
            recalculateAverageLocation();
        }
        if (averageLocation == null) {
            return new Location(Bukkit.getWorlds().getFirst(), 0, 0, 0);
        }
        return averageLocation;
    }

    public ExplorationSet parent() {
        return parent;
    }

    public void addChild(ExplorationSet child) {
        if (children == null) {
            children = Set.of();
        }
        children.add(child);
    }

    public Explorable getClosest(Location location) {
        Explorable closest = null;
        double distance = Double.MAX_VALUE;
        for (Explorable entry : entries) {
            if (entry.location().getWorld() != location.getWorld()) continue;
            double d = entry.location().distanceSquared(location);
            if (d < distance) {
                distance = d;
                closest = entry;
            }
        }
        return closest;
    }

    public Explorable getClosestUnexplored(Location location, Set<Explorable> alreadyExplored) {
        Explorable closest = null;
        double distance = Double.MAX_VALUE;
        for (Explorable entry : entries) {
            if (alreadyExplored.contains(entry)) continue;
            if (entry.location().getWorld() != location.getWorld()) continue;
            double d = entry.location().distanceSquared(location);
            if (d < distance) {
                distance = d;
                closest = entry;
            }
        }
        return closest;
    }

    public double getClosestDistance(Location location) {
        double distance = Double.MAX_VALUE;
        for (Explorable entry : entries) {
            if (entry.location().getWorld() != location.getWorld()) continue;
            double d = entry.location().distanceSquared(location);
            if (d < distance) {
                distance = d;
            }
        }
        return Math.sqrt(distance);
    }

    /**
     * Gets an explorable by its ID from this set
     */
    public Explorable getExplorable(String id) {
        for (Explorable explorable : entries) {
            if (explorable.id().equals(id)) {
                return explorable;
            }
        }
        return null;
    }

    public void addExplorable(Explorable explorable) {
        if (explorable == null) return;
        entries.add(explorable);
        recalculateAverageLocation();
    }

    public Set<Explorable> getExplorables() {
        return Set.copyOf(entries);
    }

    /**
     * Checks if this set has been completed for a player, and rewards them if it has.
     * @param player The player to check
     */
    public void checkCompletion(QPlayer player) {
        if (isCompleted(player.getExplorer().getCompletedExplorables().get(this))) {
            for (List<QAction> actions : rewardActions.values()) {
                for (QAction action : actions) {
                    try {
                        action.play(player);
                    } catch (Exception e) {
                        FriendlyError error = new FriendlyError("ExplorationSet " + id, "Error while playing reward action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                        error.addPlayer(player);
                        error.addStacktrace(e.getStackTrace());
                        QuestsXL.get().addRuntimeError(error);
                    }
                }
            }
            player.sendMessage(Component.translatable("qxl.explorationset.completed", displayName.get()));
            player.getPlayer().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.66f, 2);
        }
    }

    /**
     * Checks if this set has been completed, given a set of completed explorables.
     * @param completedExplorables The set of completed explorables
     * @return true if all entries in this set have been completed, false otherwise
     */
    public boolean isCompleted(Set<CompletedExplorable> completedExplorables) {
        return entries.stream().allMatch(e -> completedExplorables.stream().anyMatch(c -> c.explorable().equals(e)));
    }

    /**
     * Returns the average location of all entries in this set.
     * Used for speeding up the process of finding the nearest Explorable to a player.
     */
    public void recalculateAverageLocation() {
        if (entries.isEmpty()) {
            averageLocation = null;
            return;
        }
        double x = 0;
        double y = 0;
        double z = 0;
        for (Explorable entry : entries) {
            Location loc = entry.location();
            x += loc.getX();
            y += loc.getY();
            z += loc.getZ();
        }
        x /= entries.size();
        y /= entries.size();
        z /= entries.size();
        averageLocation = new Location(entries.get(0).location().getWorld(), x, y, z);
    }

    public Set<ExplorationSet> getChildren() {
        return new HashSet<>(children);
    }

    public static ExplorationSet fromQLineConfig(QLineConfig section) {
        try {
            String id = section.getName();
            QTranslatable displayName = QTranslatable.fromString(section.getString("displayName", "qxl.explorationset." + id + ".name"));
            QTranslatable description = QTranslatable.fromString(section.getString("description", "qxl.explorationset." + id + ".description"));

            // Create the set first without parent/children to avoid circular dependencies
            ExplorationSet set = new ExplorationSet(id, null, displayName, description, new ArrayList<>());

            return set;
        } catch (Exception e) {
            FriendlyError error = new FriendlyError("ExplorationSet" + section.getName(), "Error while parsing from QLineConfig", e.getMessage(), "Check the configuration for errors");
            error.addStacktrace(e.getStackTrace());
            QuestsXL.get().addRuntimeError(error);
            return null;
        }
    }

    public QLineConfig toQLineConfig() {
        QLineConfig cfg = new QLineConfig();
        cfg.setName(id); // Set the ID as the name
        try {
            cfg.set("displayName", displayName.toString());
            cfg.set("description", description.toString());
            cfg.set("parentSet", parent != null ? parent.id() : "");

            if (children != null && !children.isEmpty()) {
                List<String> childIds = children.stream().map(ExplorationSet::id).toList();
                cfg.set("childSets", childIds.toArray(new String[0]));
            }

            if (!rewardActions.isEmpty()) {
                for (Map.Entry<Integer, List<QAction>> entry : rewardActions.entrySet()) {
                    // Lets ignore this for now
                }
            }

        } catch (Exception e) {
            FriendlyError error = new FriendlyError("ExplorationSet" + id, "Error while saving to QLineConfig", e.getMessage(), "...");
            error.addStacktrace(e.getStackTrace());
            QuestsXL.get().addRuntimeError(error);
        }
        return cfg;
    }

    public void removeExplorable(Explorable lootChest) {
        entries.remove(lootChest);
        recalculateAverageLocation();
    }
}
