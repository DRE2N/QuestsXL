package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.tools.BeamTool;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

enum BeamChange {
    SHOW,
    HIDE,
    CHANGE_COLOR
}

/**
 * Displays a guardian beam to the player.
 * the start location needs to be in frame for the player,
 * otherwise it won't get rendered
 */
public class QDisplayBeam extends QBaseAction {

    private Location start;
    private Location end;
    private BeamChange action;
    private transient BeamTool beam;

    public QDisplayBeam() {}
    public QDisplayBeam(String id) {
        this.id = id;
    }
    public QDisplayBeam(Player player, Location start, Location end, BeamChange action) {
        this.start = start;
        this.end = end;
        this.action = action;
        try {
            beam = new BeamTool(player, start, end);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void play(Player player)  {
        if (!conditions(player)) return;
        switch (action) {
            case SHOW -> {
                beam.show();
            }
            case HIDE -> {
                beam.hide();
            }
            case CHANGE_COLOR -> {
                try {
                    beam.callColorChange();
                } catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void delayedEnd(int seconds) {
        BukkitRunnable delay = new BukkitRunnable() {
            @Override
            public void run() {
                cancelBeam();
            }
        };
        delay.runTaskLater(QuestsXL.getInstance(), seconds * 20L);
    }

    public void cancelBeam() {
        cancel();
    }

    @Override
    public void cancel() {
        beam.hide();
    }

    @Override
    public Material getIcon() {
        return Material.BEACON;
    }
}
