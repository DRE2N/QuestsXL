package de.erethon.questsxl.objective;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.event.QRegionLeaveEvent;
import de.erethon.questsxl.region.QRegion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

@QLoadableDoc(
        value = "leave_region",
        description = "This objective is completed when a player leaves a specific region. QuestsXL regions are used. `/q region`",
        shortExample = "leave_region: region=region_id",
        longExample = {
                "leave_region:",
                "  region: 'region_id'"
        }
)
public class LeaveRegionObjective extends QBaseObjective {

    @QParamDoc(name = "region", description = "The ID of the region that the player must leave.", required = true)
    private QRegion region;

    @Override
    public void check(ActiveObjective active, Event e) {
        if (!(e instanceof QRegionLeaveEvent event)) return;
        if (!conditions(event.getPlayer())) return;
        if (event.getRegion() == region) {
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(event.getPlayer()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        region = QuestsXL.getInstance().getRegionManager().getByID(cfg.getString("region"));
    }
}