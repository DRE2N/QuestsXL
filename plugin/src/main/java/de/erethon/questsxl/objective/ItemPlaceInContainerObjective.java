package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.hephaestus.items.HItemStack;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.error.FriendlyError;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ItemPlaceInContainerObjective extends QBaseObjective<InventoryCloseEvent> {

    private final HItemLibrary itemLibrary = QuestsXL.get().getItemLibrary();

    @QParamDoc(name = "item", description = "The key of the item that needs to be placed. Same as in /give", required = true)
    private ResourceLocation itemID;
    @QParamDoc(name = "amount", description = "The amount of items that need to be placed", def = "1")
    private int amount = 1;
    @QParamDoc(name = "location", description = "If set, the item must be placed in a container at this location")
    private QLocation location;

    @Override
    public void check(ActiveObjective active, InventoryCloseEvent event) {
        if (!conditions((Player) event.getPlayer())) return;
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        if (location != null && inventory.getHolder(false) instanceof BlockInventoryHolder blockInventoryHolder) {
            if (!blockInventoryHolder.getBlock().getLocation().equals(location.get(blockInventoryHolder.getBlock().getLocation()))) {
                return;
            }
        }
        int amount = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack == null) continue;
            HItemStack foundStack = itemLibrary.get(stack);
            if (foundStack.getItem().getKey().equals(itemID)) {
                amount += stack.getAmount();
            }
        }
        if (amount >= this.amount) {
            checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(player));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemID = ResourceLocation.parse(cfg.getString("itemID"));
        amount = cfg.getInt("amount", 1);
        location = cfg.getQLocation("location");
        if (itemID == null || itemLibrary.get(itemID) == null) {
            QuestsXL.get().addRuntimeError(new FriendlyError(findTopParent().id(), "Invalid item", "Item " + cfg.getString("item") + " does not exist in the item library.", null));
        }
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Place item; de=Item platzieren");
    }

    @Override
    public Class<InventoryCloseEvent> getEventType() {
        return InventoryCloseEvent.class;
    }
}
