package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.hephaestus.items.HItemStack;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;

@QLoadableDoc(
        value = "use_item",
        description = "Completed when a item is used (right-clicked).",
        shortExample = "use_item: item=erethon:fancy_sword,minecraft:diamond_sword",
        longExample = {
                "use_item:",
                "  item: 'erethon:bread,minecraft:apple'",
                "  amount: 8",
                "  consume: true"
        }
)
public class ItemUseObjective extends QBaseObjective<PlayerInteractEvent> {

    private final HItemLibrary itemLibrary = QuestsXL.get().getItemLibrary();

    @QParamDoc(name = "item", description = "The key(s) of the item(s) that need to be used (comma-separated for multiple items). Same as in /give", required = true)
    private final Set<ResourceLocation> itemIDs = new HashSet<>();
    @QParamDoc(name = "amount", description = "The amount of items that need to be in the used stack. Objective progress will be increased by amount", def = "1")
    private int amount = 1;
    @QParamDoc(name = "location", description = "If set, the item must be used on the block at this location")
    private QLocation location;
    @QParamDoc(name = "consume", description = "If true, the items will be consumed on use", def = "false")
    private boolean consume = false;

    @Override
    public void check(ActiveObjective active, PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        if (!event.getAction().isRightClick()) return;
        if (!conditions(event.getPlayer())) return;
        HItemStack foundStack = itemLibrary.get(event.getItem());
        if (itemIDs.contains(foundStack.getItem().getKey())) {
            if (foundStack.getBukkitStack().getAmount() < amount) {
                return;
            }
            if (location != null && event.getClickedBlock() != null) {
                QLocation eventLocation = QLocation.fromBukkitLocation(event.getClickedBlock().getLocation());
                if (!location.equals(eventLocation)) {
                    return;
                }
            }
            if (consume) {
                foundStack.getBukkitStack().setAmount(foundStack.getBukkitStack().getAmount() - amount);
            }
            QPlayer player = plugin.getDatabaseManager().getCurrentPlayer(event.getPlayer());
            for (int i = 0; i < amount; i++) {
                checkCompletion(active, this, player);
            }
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
                if (itemLibrary.get(itemID) != null) {
                    itemIDs.add(itemID);
                } else {
                    QuestsXL.get().addRuntimeError(new FriendlyError(findTopParent().id(), "Invalid item", "Item " + trimmedItem + " does not exist in the item library.", null));
                }
            } catch (Exception e) {
                QuestsXL.get().addRuntimeError(new FriendlyError(findTopParent().id(), "Invalid item format", "Item " + trimmedItem + " has invalid format.", null));
            }
        }

        amount = cfg.getInt("amount", 1);
        location = cfg.getQLocation("location", null);
        consume = cfg.getBoolean("consume", false);
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Use item; de=Item benutzen");
    }

    @Override
    public Class<PlayerInteractEvent> getEventType() {
        return PlayerInteractEvent.class;
    }
}
