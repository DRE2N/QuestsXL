package de.erethon.questsxl.condition;

import de.erethon.aergia.Aergia;
import de.erethon.aergia.group.Group;
import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class GroupSizeCondition extends QBaseCondition {

    int min;
    int max;

    @Override
    public boolean check(QPlayer player) {
        Group group = Aergia.inst().getGroupManager().getGroup(player.getPlayer());
        if (group != null && group.getSize() >= min && group.getSize() <= max) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public boolean check(QEvent event) {
        return fail(event);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        min = cfg.getInt("min");
        max = cfg.getInt("max");
    }

}
