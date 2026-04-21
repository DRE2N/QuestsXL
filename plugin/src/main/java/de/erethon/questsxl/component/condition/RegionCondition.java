package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.livingworld.region.QRegion;

import java.util.Map;

@QLoadableDoc(
        value = "region",
        description = "Checks if the player is in a certain region. QXL-Region, not Faction-Region!",
        shortExample = "region: region=region_id",
        longExample = {
                "region:",
                "  region: region_id",
        }
)
public class RegionCondition extends QBaseCondition implements VariableProvider {

    @QParamDoc(name = "region", description = "The ID of the region.", required = true)
    QRegion region;

    @Override
    public boolean checkInternal(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return check(quester);
        }
        if (player.isInRegion(region)) {
            return success(player);
        }
        return fail(player);
    }

    /** Exposes %region_id% to child actions (onSuccess / onFail). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("region_id", new QVariable(region != null ? region.getId() : ""));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        region = QuestsXL.get().getRegionManager().getByID(cfg.getString("region"));
    }

}