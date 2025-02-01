package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;

@QLoadableDoc(
        value = "location",
        description = "Checks if the player is within a certain range of a location.",
        shortExample = "location: x=192; y=64; y=20; radius=10",
        longExample = {
                "location:",
                "  x: 192",
                "  y: 64",
                "  z: 20",
                "  radius: 10"
        }
)
public class LocationCondition extends QBaseCondition {

    @QParamDoc(name = "location", description = "The location the player has to be in range of. QLocation", required = true)
    private QLocation location;
    @QParamDoc(name = "range", description = "The radius the player has to be in.", def = "1")
    private double radius;

    @Override
    public boolean check(Quester quester) {
        if (quester.getLocation().distance(location.get(quester.getLocation())) <= radius) {
            return success(quester);
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        location = cfg.getQLocation("location");
        radius = cfg.getDouble("radius", 1);
        if (radius <= 0) {
            throw new RuntimeException("The location condition in " + cfg.getName() + " contains a negative radius.");
        }
    }

}
