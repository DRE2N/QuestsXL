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
        description = "This objective is completed when a player reaches a specific location.",
        shortExample = "location: x=64; y=64; z=64; range=5",
        longExample = {
                "location: # This is completed when the player moves five blocks up from their current location.",
                "  x: ~0",
                "  y: ~5",
                "  z: ~0"
        }
)
public class LocationObjective extends AbstractLocationBasedObjective {

    @QParamDoc(name = "location", description = "The location that the player must reach. QLocation", required = true)
    private QLocation loc; // Is in parent class, we only need this for the documentation
    @QParamDoc(name = "range", description = "How close the player has to get to the location", def = "1")
    private int range; // Is in parent class, we only need this for the documentation

    public QLocation getLocation() {
        return location;
    }

    public int getDistance() {
        return distance;
    }

    @Override
    public void check(ActiveObjective active, Event e) {
        if (!(e instanceof PlayerMoveEvent event)) return;
        if (!conditions(event.getPlayer())) return;
        if (location == null || !location.sameWorld(event.getTo())) {
            return;
        }
        if (event.getTo().distance(location.get(event.getTo())) <= distance) {
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(event.getPlayer()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        location = cfg.getQLocation("location");
        distance = cfg.getInt("range", 1);
        if (distance <= 0) {
            throw new RuntimeException("The location objective in " + cfg.getName() + " contains a negative range.");
        }
    }
}
