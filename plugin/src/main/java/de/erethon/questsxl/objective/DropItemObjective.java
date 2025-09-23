package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDropItemEvent;

@QLoadableDoc(
        value = "drop_item",
        description = "Drop an item to complete this objective. Can be cancelled.",
        shortExample = "drop_item: item=erethon:fancy_sword",
        longExample = {
                "drop_item:",
                "  item: 'erethon:fancy_sword' # Needs to be quoted due to the colon.",
        }
)
public class DropItemObjective extends QBaseObjective<EntityDropItemEvent> {
    private final HItemLibrary itemLibrary = QuestsXL.get().getItemLibrary();

    @QParamDoc(name = "item", description = "The key of the item that needs to be dropped. Same as in /give", required = true)
    private NamespacedKey itemID;

    @Override
    public void check(ActiveObjective active, EntityDropItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        HItem item = itemLibrary.get(e.getItemDrop().getItemStack()).getItem();
        if (item == null) return;
        if (item.getKey().equals(itemID)) {
            if (shouldCancelEvent) e.setCancelled(true);
            complete(active.getHolder(), this, plugin.getDatabaseManager().getCurrentPlayer(player));

        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemID = NamespacedKey.fromString(cfg.getString("item"));
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Drop Item; de=Droppe Item");
    }

    @Override
    public Class<EntityDropItemEvent> getEventType() {
        return EntityDropItemEvent.class;
    }
}
