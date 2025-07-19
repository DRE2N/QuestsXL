package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;

public class ItemUseObjective extends QBaseObjective<PlayerInteractEvent> {

    @Override
    public void check(ActiveObjective active, PlayerInteractEvent event) {

    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
    }

    @Override
    public Class<PlayerInteractEvent> getEventType() {
        return PlayerInteractEvent.class;
    }
}
