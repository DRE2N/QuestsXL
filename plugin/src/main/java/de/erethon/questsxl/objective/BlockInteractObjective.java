package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.event.QRegionLeaveEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockInteractObjective extends QBaseObjective {

    private QLocation location;

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof PlayerInteractEvent e)) return;
        if (!conditions(e.getPlayer())) return;
        if (e.getClickedBlock() == null) return;
        if (location.equals(e.getClickedBlock().getLocation())) {
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(e.getPlayer()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        location = cfg.getQLocation("location");
    }
}
