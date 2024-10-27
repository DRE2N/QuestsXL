package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemConsumeEvent;

@QLoadableDoc(
        value = "consume_item",
        description = "An item needs to be consumed to complete this objective. Hephaestus item keys are used.",
        shortExample = "consume_item: item=minecraft:apple",
        longExample = {
                "consume_item:",
                "  item: 'minecraft:apple' # Needs to be quoted due to the colon."
        }
)
public class ConsumeItemObjective extends QBaseObjective {

    private final HItemLibrary itemLibrary = QuestsXL.getInstance().getItemLibrary();

    @QParamDoc(name = "item", description = "The key of the item that needs to be consumed. Same as in /give", required = true)
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
