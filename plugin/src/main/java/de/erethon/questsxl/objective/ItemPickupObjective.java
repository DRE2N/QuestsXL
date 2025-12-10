package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.HashSet;
import java.util.Set;

@QLoadableDoc(
        value = "pickup_item",
        description = "This objective is completed when the player picks up a specific item. Can be cancelled, preventing the item from being picked up.",
        shortExample = "pickup_item: item=erethon:fancy_sword,minecraft:diamond",
        longExample = {
                "pickup_item:",
                "  item: 'erethon:fancy_sword,minecraft:diamond' # Needs to be quoted due to the colon.",
                "  cancel: true"
        }
)
public class ItemPickupObjective extends QBaseObjective<EntityPickupItemEvent> {

    private final HItemLibrary itemLibrary = QuestsXL.get().getItemLibrary();

    @QParamDoc(name = "item", description = "The key(s) of the item(s) that need to be picked up (comma-separated for multiple items). Same as in /give", required = true)
    private final Set<ResourceLocation> itemIDs = new HashSet<>();

    @Override
    public void check(ActiveObjective active, EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        HItem item = itemLibrary.get(e.getItem().getItemStack()).getItem();
        if (item == null) return;
        if (itemIDs.contains(item.getKey())) {
            if (shouldCancelEvent) e.setCancelled(true);
            checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer((Player) e.getEntity()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String itemStr = cfg.getString("item");
        if (itemStr == null || itemStr.trim().isEmpty()) {
            return;
        }

        String[] items = itemStr.split(",");
        for (String item : items) {
            String trimmedItem = item.trim();
            try {
                ResourceLocation itemID = ResourceLocation.parse(trimmedItem);
                itemIDs.add(itemID);
            } catch (Exception e) {
                // Handle invalid resource location format
            }
        }
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Pick up item; de=Hebe Item auf");
    }

    @Override
    public Class<EntityPickupItemEvent> getEventType() {
        return EntityPickupItemEvent.class;
    }
}
