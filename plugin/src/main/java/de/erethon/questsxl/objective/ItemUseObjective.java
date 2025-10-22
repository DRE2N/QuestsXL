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

@QLoadableDoc(
        value = "use_item",
        description = "Completed when a item is used (right-clicked).",
        shortExample = "use_item: item=erethon:fancy_sword",
        longExample = {
                "place_item:",
                "  item: 'erethon:bread'",
                "  amount: 8",
                "  consume: true"
        }
)
public class ItemUseObjective extends QBaseObjective<PlayerInteractEvent> {

    private final HItemLibrary itemLibrary = QuestsXL.get().getItemLibrary();

    @QParamDoc(name = "item", description = "The key of the item that needs to be used. Same as in /give", required = true)
    private ResourceLocation itemID;
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
        if (foundStack.getItem().getKey().equals(itemID)) {
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
        itemID = ResourceLocation.parse(cfg.getString("itemID"));
        amount = cfg.getInt("amount", 1);
        location = cfg.getQLocation("location", null);
        consume = cfg.getBoolean("consume", false);
        if (itemID == null || itemLibrary.get(itemID) == null) {
            QuestsXL.get().addRuntimeError(new FriendlyError(findTopParent().id(), "Invalid item", "Item " + cfg.getString("item") + " does not exist in the item library.", null));
        }
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
