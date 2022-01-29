package de.erethon.questsxl.condition;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class LookingAtCondition extends QBaseCondition {

    Location locTarget;
    double accuracy;
    Material block;

    @Override
    public boolean check(QPlayer qp) {
        Player player = qp.getPlayer();
        Block target = player.getTargetBlock(32);
        if (target == null) {
            return fail(qp);
        }
        if (block != null) {
            if (target.getType().equals(block)) {
                return success(qp);
            }
            return fail(qp);
        }
        if (target.getLocation().distance(locTarget) < accuracy) {
            return success(qp);
        }
        return fail(qp);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        if (section.contains("block")) {
            block = Material.valueOf(section.getString("block"));
            return;
        }
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        if (world == null) {
            MessageUtil.log("The condition " + section.getName() + " contains a location for a world that is not loaded.");
            return;
        }
        accuracy = section.getDouble("accuracy");
        locTarget = new Location(Bukkit.getWorld(world), x, y, z);
    }

    @Override
    public void load(String[] msg) {

    }
}
