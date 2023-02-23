package de.erethon.questsxl.objective;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.event.QRegionLeaveEvent;
import de.erethon.questsxl.region.QRegion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

public class LeaveRegionObjective extends QBaseObjective {

    QRegion region;

    @Override
    public void check(ActiveObjective active, Event e) {
        if (!(e instanceof QRegionLeaveEvent event)) return;
        if (!conditions(event.getPlayer())) return;
        if (event.getRegion() == region) {
            complete(active.getHolder(), this);
        }
    }

    @Override
    public void load(QLineConfig section) {
        region = QuestsXL.getInstance().getRegionManager().getByID(section.getString("id"));
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        region = QuestsXL.getInstance().getRegionManager().getByID(section.getString("id"));
    }
}