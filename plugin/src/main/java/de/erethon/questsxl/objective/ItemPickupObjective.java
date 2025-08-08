package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;

@QLoadableDoc(
        value = "pickup_item",
        description = "This objective is completed when the player picks up a specific item. Can be cancelled, preventing the item from being picked up.",
        shortExample = "pickup_item: item=erethon:fancy_sword",
        longExample = {
                "pickup_item:",
                "  item: 'erethon:fancy_sword' # Needs to be quoted due to the colon.",
                "  cancel: true"
        }
)
public class ItemPickupObjective extends QBaseObjective<EntityPickupItemEvent> {

    private final HItemLibrary itemLibrary = QuestsXL.get().getItemLibrary();

    @QParamDoc(name = "item", description = "The key of the item that needs to be picked up. Same as in /give", required = true)
    private NamespacedKey itemID;

    @Override
    public void check(ActiveObjective active, EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        HItem item = itemLibrary.get(e.getItem().getItemStack()).getItem();
        if (item == null) return;
        if (item.getKey().equals(itemID)) {
            if (shouldCancelEvent) e.setCancelled(true);
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer((Player) e.getEntity()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemID = NamespacedKey.fromString(cfg.getString("item"));
    }

    @Override
    public Class<EntityPickupItemEvent> getEventType() {
        return EntityPickupItemEvent.class;
    }
}
