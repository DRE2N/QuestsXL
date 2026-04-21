package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.script.QLocation;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;

import java.util.Map;

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
public class LocationCondition extends QBaseCondition implements VariableProvider {

    @QParamDoc(name = "location", description = "The location the player has to be in range of. QLocation", required = true)
    private QLocation location;
    @QParamDoc(name = "range", description = "The radius the player has to be in.", def = "1")
    private double radius;

    private double lastDistance = 0;

    @Override
    public boolean checkInternal(Quester quester) {
        lastDistance = quester.getLocation().distance(location.get(quester.getLocation()));
        if (lastDistance <= radius) {
            return success(quester);
        }
        return fail(quester);
    }

    /** Exposes %distance% to child actions (onSuccess / onFail). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("distance", new QVariable(lastDistance));
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
