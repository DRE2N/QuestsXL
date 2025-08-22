package de.erethon.questsxl.objective;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.event.QRegionEnterEvent;
import de.erethon.questsxl.region.QRegion;

@QLoadableDoc(
        value = "enter_region",
        description = "This objective is completed when a player enters a specific region. QuestsXL regions are used, not Factions. `/q region`",
        shortExample = "enter_region: region=region_id",
        longExample = {
                "enter_region:",
                "  region: region_id"
        }
)
public class EnterRegionObjective extends QBaseObjective<QRegionEnterEvent> {

    @QParamDoc(name = "region", description = "The ID of the region that the player must enter.", required = true)
    private QRegion region;

    @Override
    public void check(ActiveObjective active, QRegionEnterEvent e) {
        if (!conditions(e.getPlayer())) {
            return;
        }
        if (e.getRegion() == region) {
            checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        region = QuestsXL.get().getRegionManager().getByID(cfg.getString("region"));
    }

    @Override
    public Class<QRegionEnterEvent> getEventType() {
        return QRegionEnterEvent.class;
    }
}
