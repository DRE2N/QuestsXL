package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.error.FriendlyError;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

@QLoadableDoc(
        value = "consume_item",
        description = "An item needs to be consumed to complete this objective. Hephaestus item keys are used. Can be cancelled.",
        shortExample = "consume_item: item=minecraft:apple",
        longExample = {
                "consume_item:",
                "  item: 'minecraft:apple' # Needs to be quoted due to the colon."
        }
)
public class ConsumeItemObjective extends QBaseObjective<PlayerItemConsumeEvent> {

    private final HItemLibrary itemLibrary = QuestsXL.get().getItemLibrary();

    @QParamDoc(name = "item", description = "The key of the item that needs to be consumed. Same as in /give", required = true)
    private ResourceLocation itemID;

    @Override
    public void check(ActiveObjective active, PlayerItemConsumeEvent e) {
        HItem item = itemLibrary.get(e.getItem()).getItem();
        if (item == null) return;
        if (item.getKey().equals(itemID)) {
            if (shouldCancelEvent) e.setCancelled(true);
            checkCompletion(active, this, getPlayerHolder(e.getPlayer()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemID = ResourceLocation.parse(cfg.getString("item"));
        if (itemID == null || itemLibrary.get(itemID) == null) {
            QuestsXL.get().addRuntimeError(new FriendlyError(id(), "Invalid item ID in consume_item objective"));
        }
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Consume item; de=Konsumiere Item");
    }

    @Override
    public Class<PlayerItemConsumeEvent> getEventType() {
        return PlayerItemConsumeEvent.class;
    }
}
