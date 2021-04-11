package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.players.QPlayer;
import de.erethon.questsxl.players.QPlayerCache;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class GroupSizeCondition extends QBaseCondition {

    int min;
    int max;

    @Override
    public boolean check(QPlayer player) {
        QPlayerCache cache = QuestsXL.getInstance().getPlayerCache();
        if (!cache.isInGroup(player)) {
            return false;
        }
        return cache.getGroup(player).getMembers().size() >= min && cache.getGroup(player).getMembers().size() <= max;
    }


    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
    }
}
