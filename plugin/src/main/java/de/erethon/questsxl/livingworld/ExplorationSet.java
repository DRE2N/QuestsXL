package de.erethon.questsxl.livingworld;

import com.google.gson.JsonObject;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;

import java.util.HashMap;
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
    private final ExplorationSet parent;
    private final QTranslatable displayName;
    private final QTranslatable description;
    private final List<Explorable> entries;
    private Location averageLocation;

    private Map<Integer, List<QAction>> rewardActions = new HashMap<>();

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

    public List<Explorable> entries() {
        return entries;
    }

    public Location averageLocation() {
        return averageLocation;
    }

    public ExplorationSet parent() {
        return parent;
    }

    public Explorable getClosest(Location location) {
        Explorable closest = null;
        double distance = Double.MAX_VALUE;
        for (Explorable entry : entries) {
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
            double d = entry.location().distanceSquared(location);
            if (d < distance) {
                distance = d;
            }
        }
        return Math.sqrt(distance);
    }

    public Explorable getExplorable(String id) {
        return entries.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
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
                        QuestsXL.getInstance().addRuntimeError(error);
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
        averageLocation = new Location(entries.getFirst().location().getWorld(), x, y, z);
    }

}
