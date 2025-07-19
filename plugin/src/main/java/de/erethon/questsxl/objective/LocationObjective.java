package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Arrays;

@QLoadableDoc(
        value = "location",
        description = "This objective is completed when a player reaches a specific location. Can be cancelled, preventing the player from moving closer",
        shortExample = "location: x=64; y=64; z=64; range=5",
        longExample = {
                "location: # This is completed when the player moves five blocks up from their current location.",
                "  x: ~0",
                "  y: ~5",
                "  z: ~0"
        }
)
public class LocationObjective extends QBaseObjective<PlayerMoveEvent> {

    @QParamDoc(name = "location", description = "The location that the player must reach. QLocation", required = true)
    private QLocation loc;
    @QParamDoc(name = "range", description = "How close the player has to get to the location", def = "1")
    private int range;

    public QLocation getLocation() {
        return loc;
    }

    @Override
    public void check(ActiveObjective active, PlayerMoveEvent e) {
        if (!conditions(e.getPlayer())) return;
        if (loc == null || !loc.sameWorld(e.getTo())) {
            return;
        }
        if (e.getTo().distance(loc.get(e.getTo())) <= range) {
            if (shouldCancelEvent) e.setCancelled(true);
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(e.getPlayer()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        loc = cfg.getQLocation("location");
        range = cfg.getInt("range", 1);
        if (range <= 0) {
            throw new RuntimeException("The location objective in " + cfg.getName() + " contains a negative range.");
        }
    }

    @Override
    public Class<PlayerMoveEvent> getEventType() {
        return PlayerMoveEvent.class;
    }
}
