package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDropItemEvent;

public class DropItemObjective extends QBaseObjective {
    private final HItemLibrary itemLibrary = QuestsXL.getInstance().getItemLibrary();

    private NamespacedKey itemID;
    private boolean cancel;

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof EntityDropItemEvent e)) return;
        if (!(e.getEntity() instanceof Player)) return;
        HItem item = itemLibrary.get(e.getItemDrop().getItemStack()).getItem();
        if (item == null) return;
        if (item.getKey().equals(itemID)) {
            complete(active.getHolder(), this);
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
