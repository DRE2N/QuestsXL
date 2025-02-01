package de.erethon.questsxl.condition;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "looking_at",
        description = "Checks if the player is looking at a certain location or block.",
        shortExample = "looking_at: x=192; y=64; y=20; accuracy=1",
        longExample = {
                "looking_at:",
                "  block: DIAMOND_BLOCK",
        }
)
public class LookingAtCondition extends QBaseCondition {

    @QParamDoc(name = "location", description = "The location the player has to be looking at. QLocation")
    QLocation locTarget;
    @QParamDoc(name = "accuracy", description = "The accuracy the player has to be looking at the location.", def = "1")
    double accuracy;
    @QParamDoc(name = "block", description = "The block the player has to be looking at. Can be used together with location")
    Material block;

    @Override
    public boolean check(Quester quester) {
        if (!(quester instanceof QPlayer qp)) {
            return fail(quester);
        }
        Player player = qp.getPlayer();
        Block target = player.getTargetBlockExact(32);
        if (target == null) {
            return fail(qp);
        }
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
