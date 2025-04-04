package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.tool.BeamTool;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

enum BeamChange {
    SHOW,
    HIDE,
    CHANGE_COLOR
}

@QLoadableDoc(
        value = "display_marker",
        description = "**Currently not implemented**"
)
public class DisplayLocationMarkerAction extends QBaseAction {

    private Location start;
    private Location end;
    private BeamChange action;
    private transient BeamTool beam;

    public DisplayLocationMarkerAction() {}
    public DisplayLocationMarkerAction(String id) {
        this.id = id;
    }
    public DisplayLocationMarkerAction(Player player, Location start, Location end, BeamChange action) {
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
    public void play(Quester quester)  {
        if (!conditions(quester)) return;
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
