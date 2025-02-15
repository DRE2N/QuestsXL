package de.erethon.questsxl.livingworld;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ContentGuide {

    char LEFT = '←';
    char FORWARD_RIGHT = '⬈';
    char RIGHT = '→';
    char FORWARD_LEFT = '⬉';
    char BACKWARD = '↓';
    char BACKWARD_RIGHT = '⬊';
    char FORWARD = '↑';
    char BACKWARD_LEFT = '⬋';
    char UNKNOWN = '-';
    char UP = '▲';
    char DOWN = '▼';
    char SAME = '◆';

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
        task.runTaskTimer(QuestsXL.getInstance(), 0, 25);
    }

    private void update() {
        if (explorer.getCurrentClosestSet() == null || explorer.hasCompletedSet(explorer.getCurrentClosestSet())) {
            return;
        }
        Explorable closest = getClosestInSet(explorer.getCurrentClosestSet());
        if (closest == null) {
            return;
        }
        Component hint = Component.text(getDirectionalMarker(closest.location()) + " ", NamedTextColor.DARK_PURPLE);
        hint = hint.append(Component.translatable("qxl.explorable.undiscovered"));
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

    public char getDirectionalMarker(Location toLoc) {
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