package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;

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
public class VelocityCondition extends QBaseCondition {

    @QParamDoc(description = "The minimum velocity in the x direction.", def = "0")
    private double x;
    @QParamDoc(description = "The minimum velocity in the y direction.", def = "0")
    private double y;
    @QParamDoc(description = "The minimum velocity in the z direction.", def = "0")
    private double z;

    @Override
    public boolean check(Quester quester) {
        if (quester instanceof QPlayer player) {
            Player p = player.getPlayer();
            if (Math.abs(p.getVelocity().getX()) >= x && Math.abs(p.getVelocity().getY()) >= y && Math.abs(p.getVelocity().getZ()) >= z) {
                return success(quester);
            }
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        x = cfg.getDouble("x", 0);
        y = cfg.getDouble("y", 0);
        z = cfg.getDouble("z", 0);
    }
}
