package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.error.FriendlyError;
import net.minecraft.resources.Identifier;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import java.util.HashSet;
import java.util.Set;

@QLoadableDoc(
        value = "consume_item",
        description = "An item needs to be consumed to complete this objective. Hephaestus item keys are used. Can be cancelled.",
        shortExample = "consume_item: item=minecraft:apple,minecraft:bread",
        longExample = {
                "consume_item:",
                "  item: 'minecraft:apple,minecraft:bread' # Needs to be quoted due to the colon."
        }
)
public class ConsumeItemObjective extends QBaseObjective<PlayerItemConsumeEvent> {

    private final HItemLibrary itemLibrary = QuestsXL.get().getItemLibrary();

    @QParamDoc(name = "item", description = "The key(s) of the item(s) that need to be consumed (comma-separated for multiple items). Same as in /give", required = true)
    private final Set<Identifier> itemIDs = new HashSet<>();

    @Override
    public void check(ActiveObjective active, PlayerItemConsumeEvent e) {
        HItem item = itemLibrary.get(e.getItem()).getItem();
        if (item == null) return;
        if (itemIDs.contains(item.getKey())) {
            if (shouldCancelEvent) e.setCancelled(true);
            checkCompletion(active, this, getPlayerHolder(e.getPlayer()));
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
                Identifier itemID = Identifier.parse(trimmedItem);
                if (itemLibrary.get(itemID) != null) {
                    itemIDs.add(itemID);
                } else {
                    QuestsXL.get().addRuntimeError(new FriendlyError(id(), "Invalid item ID in consume_item objective: " + trimmedItem));
                }
            } catch (Exception e) {
                QuestsXL.get().addRuntimeError(new FriendlyError(id(), "Invalid item ID format in consume_item objective: " + trimmedItem));
            }
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
