package de.erethon.questsxl.condition;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class LookingAtCondition extends QBaseCondition {

    QLocation locTarget;
    double accuracy;
    Material block;

    @Override
    public boolean check(QPlayer qp) {
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
    public boolean check(QEvent event) {
        return fail(event);
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
