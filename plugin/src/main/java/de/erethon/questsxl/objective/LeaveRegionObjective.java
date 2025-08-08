package de.erethon.questsxl.objective;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.event.QRegionLeaveEvent;
import de.erethon.questsxl.region.QRegion;

@QLoadableDoc(
        value = "leave_region",
        description = "This objective is completed when a player leaves a specific region. QuestsXL regions are used. `/q region`",
        shortExample = "leave_region: region=region_id",
        longExample = {
                "leave_region:",
                "  region: 'region_id'"
        }
)
public class LeaveRegionObjective extends QBaseObjective<QRegionLeaveEvent> {

    @QParamDoc(name = "region", description = "The ID of the region that the player must leave.", required = true)
    private QRegion region;

    @Override
    public void check(ActiveObjective active, QRegionLeaveEvent e) {
        if (!conditions(e.getPlayer())) return;
        if (e.getRegion() == region) {
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(e.getPlayer()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        region = QuestsXL.get().getRegionManager().getByID(cfg.getString("region"));
    }

    @Override
    public Class<QRegionLeaveEvent> getEventType() {
        return QRegionLeaveEvent.class;
    }
}