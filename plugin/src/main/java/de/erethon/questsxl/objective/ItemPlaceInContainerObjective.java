package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class ItemPlaceInContainerObjective extends QBaseObjective<InventoryCloseEvent> {
    @Override
    public void check(ActiveObjective active, InventoryCloseEvent event) {

    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Place item; de=Item platzieren");
    }

    @Override
    public Class<InventoryCloseEvent> getEventType() {
        return InventoryCloseEvent.class;
    }
}
