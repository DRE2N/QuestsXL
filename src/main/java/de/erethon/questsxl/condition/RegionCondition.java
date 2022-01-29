package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.region.QRegion;
import org.bukkit.configuration.ConfigurationSection;

public class RegionCondition extends QBaseCondition {

    QRegion region;

    @Override
    public boolean check(QPlayer player) {
        if (player.isInRegion(region)) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        region = QuestsXL.getInstance().getRegionManager().getByID(section.getString("region"));
    }

    @Override
    public void load(String[] c) {
        region = QuestsXL.getInstance().getRegionManager().getByID(c[0]);
    }
}