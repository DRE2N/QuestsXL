package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class ConsumeItemObjective extends QBaseObjective {

    private final HItemLibrary itemLibrary = QuestsXL.getInstance().getItemLibrary();

    private NamespacedKey itemID;

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof PlayerItemConsumeEvent e)) return;
        HItem item = itemLibrary.get(e.getItem()).getItem();
        if (item == null) return;
        if (item.getKey().equals(itemID)) {
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(e.getPlayer()));
        }
    }


    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemID = NamespacedKey.fromString(cfg.getString("item"));

    }
}
