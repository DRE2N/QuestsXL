package de.erethon.questsxl.condition;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import org.bukkit.configuration.ConfigurationSection;

public class GroupSizeCondition extends QBaseCondition {

    int min;
    int max;

    @Override
    public boolean check(QPlayer player) {
        QPlayerCache cache = QuestsXL.getInstance().getPlayerCache();
        if (!cache.isInGroup(player)) {
            return fail(player);
        }
        if (cache.getGroup(player).getMembers().size() >= min && cache.getGroup(player).getMembers().size() <= max) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        min = section.getInt("min");
        max = section.getInt("max");
    }

    @Override
    public void load(String[] c) {
        min = NumberUtil.parseInt(c[0]);
        max = NumberUtil.parseInt(c[1]);
    }
}
