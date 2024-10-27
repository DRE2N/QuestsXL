package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItemStack;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.fyreum.jobsxl.user.event.UserCraftItemEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
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
public class ItemCraftObjective extends QBaseObjective {

    @QParamDoc(name = "jxl_item", description = "The id of the job item that needs to be crafted.")
    private String jxlID;
    @QParamDoc(name = "item", description = "The key of the item that needs to be crafted. Hephaestus. Same as in /give")
    private NamespacedKey id;

    @Override
    public void check(ActiveObjective active, Event e) {
        if (e instanceof CraftItemEvent event && id != null) { // default crafting
            Player player = (Player) event.getWhoClicked();
            HItemStack item = plugin.getItemLibrary().get(event.getRecipe().getResult());
            if (conditions(player) && item.getItem().getKey().equals(id)) {
                checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(player));
            }
        }
        if (e instanceof UserCraftItemEvent event && jxlID != null) { // job crafting
            Player player = event.getUser().getPlayer();
            if (conditions(player) && event.getResult().getId().equalsIgnoreCase(jxlID)) {
                checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(player));
            }
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        jxlID = cfg.getString("jxl_item");
        id = NamespacedKey.fromString(cfg.getString("item"));
    }
}
