package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@QLoadableDoc(
        value = "velocity",
        description = "This condition is successful if the player's velocity is greater than the specified values.",
        shortExample = "velocity: x=0.3",
        longExample = {
                "velocity:",
                "  x: 0.5",
                "  y: 0.5",
                "  z: 0.5"
        }
)
public class VelocityCondition extends QBaseCondition implements VariableProvider {

    @QParamDoc(name = "x", description = "The minimum velocity in the x direction.", def = "0")
    private double x;
    @QParamDoc(name = "y", description = "The minimum velocity in the y direction.", def = "0")
    private double y;
    @QParamDoc(name = "z", description = "The minimum velocity in the z direction.", def = "0")
    private double z;

    private double lastVelX = 0, lastVelY = 0, lastVelZ = 0;

    @Override
    public boolean checkInternal(Quester quester) {
        if (quester instanceof QPlayer player) {
            Player p = player.getPlayer();
            lastVelX = p.getVelocity().getX();
            lastVelY = p.getVelocity().getY();
            lastVelZ = p.getVelocity().getZ();
            if (Math.abs(lastVelX) >= x && Math.abs(lastVelY) >= y && Math.abs(lastVelZ) >= z) {
                return success(quester);
            }
        }
        return fail(quester);
    }

    /** Exposes %velocity_x%, %velocity_y%, %velocity_z% to child actions (onSuccess / onFail). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        Map<String, QVariable> vars = new HashMap<>();
        vars.put("velocity_x", new QVariable(lastVelX));
        vars.put("velocity_y", new QVariable(lastVelY));
        vars.put("velocity_z", new QVariable(lastVelZ));
        return vars;
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        x = cfg.getDouble("x", 0);
        y = cfg.getDouble("y", 0);
        z = cfg.getDouble("z", 0);
    }
}
