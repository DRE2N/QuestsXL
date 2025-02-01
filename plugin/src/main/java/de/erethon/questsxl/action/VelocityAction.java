package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.util.Vector;

@QLoadableDoc(
        value = "velocity",
        description = "Adds velocity to a player",
        shortExample = "add_velocity: x=1; y=1.5; z=1",
        longExample = {
                "add_velocity:",
                "  x: 1",
                "  y: 2",
                "  z: 3"
        }
)
public class VelocityAction extends QBaseAction {

    @QParamDoc(name = "x", description = "The x velocity", def = "0")
    private double x;
    @QParamDoc(name = "y", description = "The y velocity", def = "0")
    private double y;
    @QParamDoc(name = "z", description = "The z velocity", def = "0")
    private double z;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester, (QPlayer qp) -> qp.getPlayer().setVelocity(new Vector(x, y, z)));
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        x = cfg.getDouble("x", 0);
        y = cfg.getDouble("y", 0);
        z = cfg.getDouble("z", 0);
    }
}
