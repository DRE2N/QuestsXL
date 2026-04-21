package de.erethon.questsxl.component.action;

import de.erethon.questsxl.common.script.ExecutionContext;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
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

    @QParamDoc(name = "x", description = "The x velocity — supports %variables%", def = "0")
    private String rawX;
    @QParamDoc(name = "y", description = "The y velocity — supports %variables%", def = "0")
    private String rawY;
    @QParamDoc(name = "z", description = "The z velocity — supports %variables%", def = "0")
    private String rawZ;

    @Override
    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        ExecutionContext ctx = ExecutionContext.current();
        double x = ctx != null ? ctx.resolveDouble(rawX) : parseDouble(rawX);
        double y = ctx != null ? ctx.resolveDouble(rawY) : parseDouble(rawY);
        double z = ctx != null ? ctx.resolveDouble(rawZ) : parseDouble(rawZ);
        execute(quester, (QPlayer qp) -> qp.getPlayer().setVelocity(new Vector(x, y, z)));
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        rawX = cfg.getString("x", "0");
        rawY = cfg.getString("y", "0");
        rawZ = cfg.getString("z", "0");
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }
}
