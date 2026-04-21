package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.script.QLocation;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@QLoadableDoc(
        value = "looking_at",
        description = "Checks if the player is looking at a certain location or block.",
        shortExample = "looking_at: x=192; y=64; y=20; accuracy=1",
        longExample = {
                "looking_at:",
                "  block: DIAMOND_BLOCK",
        }
)
public class LookingAtCondition extends QBaseCondition implements VariableProvider {

    @QParamDoc(name = "location", description = "The location the player has to be looking at. QLocation")
    QLocation locTarget;
    @QParamDoc(name = "accuracy", description = "The accuracy the player has to be looking at the location.", def = "1")
    double accuracy;
    @QParamDoc(name = "block", description = "The block the player has to be looking at. Can be used together with location")
    Material block;

    private int lastX = 0, lastY = 0, lastZ = 0;
    private String lastBlockType = "";

    @Override
    public boolean checkInternal(Quester quester) {
        if (!(quester instanceof QPlayer qp)) {
            return fail(quester);
        }
        Player player = qp.getPlayer();
        Block target = player.getTargetBlockExact(32);
        if (target == null) {
            return fail(qp);
        }
        lastX = target.getX();
        lastY = target.getY();
        lastZ = target.getZ();
        lastBlockType = target.getType().name().toLowerCase();
        if (block != null) {
            if (target.getType().equals(block)) {
                return success(qp);
            }
            return fail(qp);
        }
        if (target.getLocation().distance(locTarget.get(qp.getLocation())) < accuracy) {
            return success(qp);
        }
        return fail(qp);
    }

    /** Exposes %looking_at_x%, %looking_at_y%, %looking_at_z%, %looking_at_block% to child actions. */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        Map<String, QVariable> vars = new HashMap<>();
        vars.put("looking_at_x", new QVariable(lastX));
        vars.put("looking_at_y", new QVariable(lastY));
        vars.put("looking_at_z", new QVariable(lastZ));
        vars.put("looking_at_block", new QVariable(lastBlockType));
        return vars;
    }


    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        if (cfg.contains("block")) {
            block = Material.valueOf(cfg.getString("block"));
            return;
        }
        accuracy = cfg.getDouble("accuracy", 1);
        locTarget = cfg.getQLocation("location");
    }

}
