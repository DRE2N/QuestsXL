package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.region.QRegion;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "region",
        description = "Checks if the player is in a certain region. QXL-Region, not Faction-Region!",
        shortExample = "'region: region=region_id'",
        longExample = {
                "region:",
                "  region: region_id",
        }
)
public class RegionCondition extends QBaseCondition {

    @QParamDoc(name = "region", description = "The ID of the region.", required = true)
    QRegion region;

    @Override
    public boolean check(QPlayer player) {
        if (player.isInRegion(region)) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public boolean check(QEvent event) {
        if (QuestsXL.getInstance().getRegionManager().getByLocation(event.getCenterLocation()) == region) {
            return success(event);
        }
        return fail(event);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        region = QuestsXL.getInstance().getRegionManager().getByID(cfg.getString("region"));
    }

}