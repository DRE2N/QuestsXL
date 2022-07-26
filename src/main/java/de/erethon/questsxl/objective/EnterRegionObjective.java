package de.erethon.questsxl.objective;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.event.QRegionEnterEvent;
import de.erethon.questsxl.region.QRegion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

public class EnterRegionObjective extends QBaseObjective {

    QRegion region;

    @Override
    public void check(Event e) {
        if (!(e instanceof QRegionEnterEvent event)) return;
        MessageUtil.log(event.getPlayer() + " has entered RegionObjective with " + event.getRegion().getId() + ". Checking for " + region.getId());
        if (!conditions(event.getPlayer())) {
            return;
        }
        if (event.getRegion() == region) {
            complete(plugin.getPlayerCache().getByPlayer(event.getPlayer()), this);
        }
    }

    public void load(String[] c) {
        super.load(c);
        region = QuestsXL.getInstance().getRegionManager().getByID(c[0]);
    }

    public void load(ConfigurationSection section) {
        super.load(section);
        region = QuestsXL.getInstance().getRegionManager().getByID(section.getString("id"));
    }
}
