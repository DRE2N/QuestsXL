package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDropItemEvent;

@QLoadableDoc(
        value = "drop_item",
        description = "Drop an item to complete this objective. Can optionally cancel the drop event, preventing the item from being dropped.",
        shortExample = "drop_item: item=erethon:fancy_sword",
        longExample = {
                "drop_item:",
                "  item: 'erethon:fancy_sword' # Needs to be quoted due to the colon.",
                "  cancel: true"
        }
)
public class DropItemObjective extends QBaseObjective {
    private final HItemLibrary itemLibrary = QuestsXL.getInstance().getItemLibrary();

    @QParamDoc(name = "item", description = "The key of the item that needs to be dropped. Same as in /give", required = true)
    private NamespacedKey itemID;
    @QParamDoc(name = "cancel", description = "Whether the drop event should be cancelled.", def = "false")
    private boolean cancel;

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof EntityDropItemEvent e)) return;
        if (!(e.getEntity() instanceof Player player)) return;
        HItem item = itemLibrary.get(e.getItemDrop().getItemStack()).getItem();
        if (item == null) return;
        if (item.getKey().equals(itemID)) {
            complete(active.getHolder(), this, plugin.getPlayerCache().getByPlayer(player));
            if (cancel) {
                e.setCancelled(true);
            }
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemID = NamespacedKey.fromString(cfg.getString("item"));
        cancel = cfg.getBoolean("cancel", false);
    }
}
