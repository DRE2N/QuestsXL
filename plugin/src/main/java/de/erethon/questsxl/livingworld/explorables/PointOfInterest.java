package de.erethon.questsxl.livingworld.explorables;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.Explorable;
import de.erethon.questsxl.livingworld.ExplorationSet;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* Represents a point of interest in the world that can be discovered by players.
* Points of interest can have actions and conditions attached to them that are run
* when a player discovers the POI.
 */
public class PointOfInterest implements QComponent, Explorable {

    private String id;
    private QTranslatable displayName;
    private QTranslatable flavourText;
    private ExplorationSet set;
    private Location location;
    private double radius = 3;

    private Map<Integer, List<QAction>> rewardActions = new HashMap<>();
    private Set<QCondition> conditions = new HashSet<>();

    /**
     * Attempts to discover the POI for the given player.
     * Checks if the player is close enough to the POI and if all conditions are met.
     * @param player The player attempting to discover the POI
     * @return true if the player discovered the POI
     */
    public boolean attemptDiscovery(QPlayer player) {
        if (player.getLocation().distance(location) > radius) { // Just in case its forgotten elsewhere
            return false;
        }
        for (QCondition condition : conditions) {
            if (!condition.check(player)) {
                return false;
            }
        }
        player.getExplorer().completeExplorable(set, this, System.currentTimeMillis());
        player.getPlayer().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.66f, 2);
        for (List<QAction> actions : rewardActions.values()) {
            for (QAction action : actions) {
                action.play(player);
            }
        }
        return true;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public QTranslatable displayName() {
        return displayName;
    }

    @Override
    public Location location() {
        return location;
    }

    public static PointOfInterest fromQLineConfig(QLineConfig section) {
        try {
            PointOfInterest poi = new PointOfInterest();
            poi.id = section.getName();
            poi.set = QuestsXL.getInstance().getExploration().getSet(section.getString("parentSet"));
            if (poi.set == null) {
                throw new IllegalArgumentException("Parent set not found");
            }
            poi.displayName = QTranslatable.fromString(section.getString("displayName", "<missing translation>"));
            poi.flavourText = QTranslatable.fromString(section.getString("flavourText", "<missing translation>"));
            double x = section.getDouble("location.x");
            double y = section.getDouble("location.y");
            double z = section.getDouble("location.z");
            World world = Bukkit.getWorld(section.getString("location.world", "Erethon"));
            poi.location = new Location(world, x, y, z);
            poi.radius = section.getDouble("radius", 3);
            return poi;
        } catch (Exception e) {
            FriendlyError error = new FriendlyError("POI" + section.getName(), "Error while parsing from QLineConfig", e.getMessage(), "Check the configuration for errors");
            error.addStacktrace(e.getStackTrace());
            QuestsXL.getInstance().addRuntimeError(error);
            return null;
        }
    }

    public QLineConfig toQLineConfig() {
        QLineConfig cfg = new QLineConfig();
        try {
            cfg.set("parentSet", set.id());
            cfg.set("displayName", displayName.toString());
            cfg.set("flavourText", flavourText.toString());
            cfg.set("location.x", location.getX());
            cfg.set("location.y", location.getY());
            cfg.set("location.z", location.getZ());
            cfg.set("location.world", location.getWorld().getName());
            cfg.set("radius", radius);
        } catch (Exception e) {
            FriendlyError error = new FriendlyError("POI" + id, "Error while saving to QLineConfig", e.getMessage(), "...");
            error.addStacktrace(e.getStackTrace());
            QuestsXL.getInstance().addRuntimeError(error);
        }
        return cfg;
    }
}
