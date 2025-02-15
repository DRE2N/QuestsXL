package de.erethon.questsxl.livingworld;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

public class ContentGuide {

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
        Component hint = Component.text(getDirectionalArrow(closest.location()) + " ", NamedTextColor.DARK_PURPLE);
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

    private char getDirectionalArrow(Location location) {
        Location playerLoc = player.getPlayer().getLocation();
        double angle = Math.toDegrees(Math.atan2(location.getZ() - playerLoc.getZ(), location.getX() - playerLoc.getX()));
        angle -= playerLoc.getYaw();
        if (angle < 0) {
            angle += 360;
        }
        if (angle >= 337.5 || angle < 22.5) {
            return '↑';
        }
        if (angle < 67.5) {
            return '↗';
        }
        if (angle < 112.5) {
            return '→';
        }
        if (angle < 157.5) {
            return '↘';
        }
        if (angle < 202.5) {
            return '↓';
        }
        if (angle < 247.5) {
            return '↙';
        }
        if (angle < 292.5) {
            return '←';
        }
        return '↖';
    }
}