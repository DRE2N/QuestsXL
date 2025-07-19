package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItemStack;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.CraftItemEvent;

@QLoadableDoc(
        value = "craft",
        description = "An item needs to be crafted to complete this objective. Supports both vanilla crafting as well as JobsXL crafting. If both are set, both can be used to complete the objective.",
        shortExample = "craft: jxl_item=fancy_sword",
        longExample = {
                "craft:",
                "  item: 'erethon:fancy_sword' # Needs to be quoted due to the colon."
        }
)
public class ItemCraftObjective extends QBaseObjective<CraftItemEvent> {

    @QParamDoc(name = "item", description = "The key of the item that needs to be crafted. Hephaestus. Same as in /give")
    private NamespacedKey id;

    @Override
    public void check(ActiveObjective active, CraftItemEvent e) {
        Player player = (Player) e.getWhoClicked();
        HItemStack item = plugin.getItemLibrary().get(e.getRecipe().getResult());
        if (conditions(player) && item.getItem().getKey().equals(id)) {
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(player));
        }

    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        id = NamespacedKey.fromString(cfg.getString("item"));
    }

    @Override
    public Class<CraftItemEvent> getEventType() {
        return CraftItemEvent.class;
    }
}
