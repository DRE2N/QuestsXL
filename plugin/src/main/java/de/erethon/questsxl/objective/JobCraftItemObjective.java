package de.erethon.questsxl.objective;

import de.erethon.hephaestus.events.HJobCraftItemEvent;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Player;

@QLoadableDoc(
    value = "job_craft_item",
    shortExample = "job_craft_item: item=erethon:sword; recipe=erethon:sword_recipe; minLevel=2",
    longExample = """
        job_craft_item:
          item: erethon:sword
          recipe: erethon:sword_recipe
          minLevel: 2
        """
)
public class JobCraftItemObjective extends QBaseObjective<HJobCraftItemEvent> {

    @QParamDoc(name = "item", description = "The ID of the item to be crafted.", required = true)
    private String itemId;
    @QParamDoc(name = "recipe", description = "The ID of the recipe to be used.")
    private String recipeId;
    @QParamDoc(name = "minLevel", description = "The minimum level of the crafted item.")
    private int minResultLevel = 0;

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("de=Stelle ein " + itemId + " her.; en=Craft a " + itemId + ".");
    }

    @Override
    public Class<HJobCraftItemEvent> getEventType() {
        return HJobCraftItemEvent.class;
    }

    @Override
    public void check(ActiveObjective activeObjective, HJobCraftItemEvent event) {
        if (!conditions(event.getPlayer())) {
            return;
        }
        if (!event.getResultItemId().equalsIgnoreCase(itemId)) {
            return;
        }
        if (recipeId != null && !recipeId.isEmpty() && !event.getRecipe().getId().equalsIgnoreCase(recipeId)) {
            return;
        }
        if (event.getResultLevel() < minResultLevel) {
            return;
        }
        complete(getPlayerHolder(event.getPlayer()), this, getPlayerHolder(event.getPlayer()));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemId = cfg.getString("item");
        recipeId = cfg.getString("recipe");
        minResultLevel = cfg.getInt("minLevel", 0);
    }
}
