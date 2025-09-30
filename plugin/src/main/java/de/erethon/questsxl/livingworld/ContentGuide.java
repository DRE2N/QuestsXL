package de.erethon.questsxl.livingworld;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.explorables.PointOfInterest;
import de.erethon.questsxl.livingworld.explorables.LootChest;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.respawn.RespawnPoint;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ContentGuide {

    static char LEFT = '←';
    static char FORWARD_RIGHT = '⬈';
    static char RIGHT = '→';
    static char FORWARD_LEFT = '⬉';
    static char BACKWARD = '↓';
    static char BACKWARD_RIGHT = '⬊';
    static char FORWARD = '↑';
    static char BACKWARD_LEFT = '⬋';
    static char UNKNOWN = '-';
    static char UP = '▲';
    static char DOWN = '▼';
    static char SAME = '◆';

    public static final double MAX_DISTANCE_FOR_HINT = 64;

    private final QPlayer player;
    private final PlayerExplorer explorer;
    private BukkitRunnable task;
    private double distance = 0;

    public ContentGuide(QPlayer player, PlayerExplorer explorer) {
        this.player = player;
        this.explorer = explorer;
        startTask();
    }

    private void startTask() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        };
        task.runTaskTimer(QuestsXL.get(), 0, 25);
    }

    private void update() {
        Explorable closest = null;
        double closestDistance = Double.MAX_VALUE;

        // First, check for unexplored items in the current closest set
        if (explorer.getCurrentClosestSet() != null && !explorer.hasCompletedSet(explorer.getCurrentClosestSet())) {
            Explorable setClosest = getClosestUnexploredInSet(explorer.getCurrentClosestSet());
            if (setClosest != null && distance < closestDistance) {
                closest = setClosest;
                closestDistance = distance;
            }
        }

        QuestsXL plugin = QuestsXL.get();
        Location playerLoc = player.getPlayer().getLocation();

        // Check explorables in exploration sets
        for (ExplorationSet set : plugin.getExploration().getSets()) {
            if (set.averageLocation().distanceSquared(playerLoc) > 64 * 64) { // Skip sets that are too far away
                continue;
            }
            for (Explorable explorable : set.entries()) {
                if (explorable instanceof PointOfInterest poi) {
                    if (explorer.hasExplored(poi)) {
                        continue;
                    }

                    double dist = Math.sqrt(poi.location().distanceSquared(playerLoc));
                    if (dist < closestDistance && dist <= MAX_DISTANCE_FOR_HINT) {
                        closest = poi;
                        closestDistance = dist;
                    }
                } else if (explorable instanceof LootChest chest) {
                    // Check if the chest has already been looted by this player
                    if (plugin.getLootChestManager().hasPlayerLootedChest(player, chest.id())) {
                        continue;
                    }

                    double dist = Math.sqrt(chest.location().distanceSquared(playerLoc));
                    if (dist < closestDistance && dist <= MAX_DISTANCE_FOR_HINT) {
                        closest = chest;
                        closestDistance = dist;
                    }
                } else if (explorable instanceof RespawnPoint respawnPoint) {
                    // Check if the respawn point is visible and not yet unlocked
                    if (!respawnPoint.isVisibleTo(player) || respawnPoint.isUnlockedFor(player)) {
                        continue;
                    }

                    double dist = Math.sqrt(respawnPoint.location().distanceSquared(playerLoc));
                    if (dist < closestDistance && dist <= MAX_DISTANCE_FOR_HINT) {
                        closest = respawnPoint;
                        closestDistance = dist;
                    }
                }
            }
        }

        // Check standalone explorables (including standalone respawn points)
        for (Explorable explorable : plugin.getExploration().getStandaloneExplorables()) {
            if (explorable.location().distanceSquared(playerLoc) > 64 * 64) { // Skip if too far away
                continue;
            }

            if (explorable instanceof RespawnPoint respawnPoint) {
                // Check if the respawn point is visible and not yet unlocked
                if (!respawnPoint.isVisibleTo(player) || respawnPoint.isUnlockedFor(player)) {
                    continue;
                }

                double dist = Math.sqrt(respawnPoint.location().distanceSquared(playerLoc));
                if (dist < closestDistance && dist <= MAX_DISTANCE_FOR_HINT) {
                    closest = respawnPoint;
                    closestDistance = dist;
                }
            } else {
                // Handle other standalone explorables
                if (explorer.hasExplored(explorable)) {
                    continue;
                }

                double dist = Math.sqrt(explorable.location().distanceSquared(playerLoc));
                if (dist < closestDistance && dist <= MAX_DISTANCE_FOR_HINT) {
                    closest = explorable;
                    closestDistance = dist;
                }
            }
        }

        // Update distance for the chosen closest explorable
        distance = closestDistance;

        if (closest == null || closestDistance > MAX_DISTANCE_FOR_HINT) {
            player.setContentGuideText(null); // Clear the content guide text when no explorable is nearby
            return;
        }

        Component hint = Component.text(getDirectionalMarker(player.getPlayer(), closest.location()) + " ", NamedTextColor.DARK_PURPLE);

        // Customize message based on explorable type
        if (closest instanceof RespawnPoint) {
            hint = hint.append(Component.translatable("qxl.explorable.respawn.nearby", closest.displayName().get()));
        } else if (closest instanceof LootChest) {
            hint = hint.append(Component.translatable("qxl.explorable.lootchest.nearby", closest.displayName().get()));
        } else {
            hint = hint.append(Component.translatable("qxl.explorable.undiscovered"));
        }

        player.setContentGuideText(hint);
    }

    private Explorable getClosestInSet(ExplorationSet set) {
        Explorable closest = set.getClosest(player.getPlayer().getLocation());
        if (closest == null) {
            distance = 0;
            return null;
        }
        distance = Math.sqrt(closest.location().distanceSquared(player.getPlayer().getLocation()));
        return closest;
    }

    private Explorable getClosestUnexploredInSet(ExplorationSet set) {
        Explorable closest = set.getClosestUnexplored(player.getPlayer().getLocation(), explorer.getExploredInSet(set));
        if (closest == null) {
            distance = 0;
            return null;
        }
        distance = Math.sqrt(closest.location().distanceSquared(player.getPlayer().getLocation()));
        return closest;
    }

    public static char getDirectionalMarker(Player player, Location toLoc) {
        Location fromLoc = player.getLocation().clone();
        if (toLoc.getWorld() != fromLoc.getWorld()) {
            return UNKNOWN;
        }
        Vector toVector = toLoc.clone().subtract(fromLoc).toVector().normalize();
        Vector fromVector = fromLoc.getDirection();
        double x1 = toVector.getX();
        double z1 = toVector.getZ();
        double x2 = fromVector.getX();
        double z2 = fromVector.getZ();
        double angle = Math.atan2(x1*z2-z1*x2, x1*x2+z1*z2) * 180 / Math.PI;

        if (angle >= -22.5 && angle <= 22.5) {
            return FORWARD;
        }
        if (angle >= 22.5 && angle <= 67.5) {
            // Looking forward right, target is more on the left
            return FORWARD_LEFT;
        }
        if (angle <= -22.5 && angle >= -67.5) {
            // Looking forward left
            return FORWARD_RIGHT;
        }
        if (angle >= 67.5 && angle <= 112.5) {
            // Looking right
            return LEFT;
        }
        if (angle <= -67.5 && angle >= -112.5) {
            // Looking left
            return RIGHT;
        }
        if (angle <= -112.5 && angle >= -157.5) {
            return BACKWARD_RIGHT;
        }
        if (angle >= 112.5 && angle <= 157.5) {
            return BACKWARD_LEFT;
        }
        if (angle >= 157.5 || angle <= -157.5) {
            return BACKWARD;
        }
        return UNKNOWN;
    }

    public char getVerticalMarker(Location toLoc) {
        Location fromLoc = player.getLocation().clone();
        int fromY = fromLoc.getBlockY();
        int toY = toLoc.getBlockY();
        if (fromY < toY) {
            return UP;
        }
        // toY + 1 because of jumping
        if (fromY > toY + 1) {
            return DOWN;
        }
        return SAME;
    }
}