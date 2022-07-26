package de.erethon.questsxl.objective;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.event.QRegionLeaveEvent;
import de.erethon.questsxl.region.QRegion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

public class LeaveRegionObjective extends QBaseObjective {

    QRegion region;

    @Override
    public void check(Event e) {
        if (!(e instanceof QRegionLeaveEvent event)) return;
        if (!conditions(event.getPlayer())) return;
        if (event.getRegion() == region) {
            complete(plugin.getPlayerCache().getByPlayer(event.getPlayer()), this);
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