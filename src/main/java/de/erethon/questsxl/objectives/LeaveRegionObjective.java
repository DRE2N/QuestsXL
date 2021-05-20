package de.erethon.questsxl.objectives;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.events.QRegionLeaveEvent;
import de.erethon.questsxl.regions.QRegion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

public class LeaveRegionObjective extends QBaseObjective {

    QRegion region;

    @Override
    public void check(Event e) {
        if (!(e instanceof QRegionLeaveEvent)) return;
        QRegionLeaveEvent event = (QRegionLeaveEvent) e;
        if (!conditions(event.getPlayer())) return;
        if (event.getRegion() == region) {
            complete(event.getPlayer(), this);
        }
    }

    @Override
    public void load(String[] c) {
        super.load(c);
        region = QuestsXL.getInstance().getRegionManager().getByID(c[0]);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        region = QuestsXL.getInstance().getRegionManager().getByID(section.getString("id"));
    }
}