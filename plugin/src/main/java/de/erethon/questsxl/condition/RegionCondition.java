package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.region.QRegion;

@QLoadableDoc(
        value = "region",
        description = "Checks if the player is in a certain region. QXL-Region, not Faction-Region!",
        shortExample = "region: region=region_id",
        longExample = {
                "region:",
                "  region: region_id",
        }
)
public class RegionCondition extends QBaseCondition {

    @QParamDoc(name = "region", description = "The ID of the region.", required = true)
    QRegion region;

    @Override
    public boolean check(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return check(quester);
        }
        if (player.isInRegion(region)) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        region = QuestsXL.get().getRegionManager().getByID(cfg.getString("region"));
    }

}