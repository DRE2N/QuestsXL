package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Player;
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
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Use item; de=Item benutzen");
    }

    @Override
    public Class<PlayerInteractEvent> getEventType() {
        return PlayerInteractEvent.class;
    }
}
