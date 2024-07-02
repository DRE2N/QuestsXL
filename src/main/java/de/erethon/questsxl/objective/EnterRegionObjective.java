package de.erethon.questsxl.objective;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.event.QRegionEnterEvent;
import de.erethon.questsxl.region.QRegion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

public class EnterRegionObjective extends QBaseObjective {

    QRegion region;

    @Override
    public void check(ActiveObjective active, Event e) {
        if (!(e instanceof QRegionEnterEvent event)) return;
        MessageUtil.log(event.getPlayer() + " has entered RegionObjective with " + event.getRegion().getId() + ". Checking for " + region.getId());
        if (!conditions(event.getPlayer())) {
            return;
        }
        if (event.getRegion() == region) {
            complete(active.getHolder(), this);
        }
    }

    public void load(QLineConfig section) {
        region = QuestsXL.getInstance().getRegionManager().getByID(section.getString("id"));
    }

    public void load(ConfigurationSection section) {
        super.load(section);
        region = QuestsXL.getInstance().getRegionManager().getByID(section.getString("id"));
    }
}
