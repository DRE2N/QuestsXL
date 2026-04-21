package de.erethon.questsxl.component.action;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemStack;
import de.erethon.questsxl.common.script.ExecutionContext;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@QLoadableDoc(
        value = "remove_item",
        description = "Removes an item from the player. The item needs to be defined in the Hephaestus item library. <br>You can check if an item exists by using `/give` in Minecraft.",
        shortExample = "remove_item: item=minecraft:stone; amount=420",
        longExample = {
                "remove_item:",
                "  item: 'minecraft:stone' # The ID needs to be quoted due to the colon",
                "  amount: 420",
        }
)
public class RemoveItemAction extends QBaseAction {

    @QParamDoc(name = "item", description = "The ID of the item to remove", required = true)
    private String itemID;
    @QParamDoc(name = "amount", description = "The amount of items to remove — supports %variables%", def="1")
    private String rawAmount = "1";

    @Override
    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        if (!(quester instanceof QPlayer qplayer)) {
            return;
        }
        ExecutionContext ctx = ExecutionContext.current();
        int amount = ctx != null ? ctx.resolveInt(rawAmount) : parseInt(rawAmount);
        execute(quester, (QPlayer player) -> {
            NamespacedKey key = NamespacedKey.fromString(itemID);
            HItem item = plugin.getItemLibrary().get(key);
            if (item == null) {
                return;
            }
            int remaining = amount;
            for (ItemStack stack : qplayer.getPlayer().getInventory().getContents()) {
                if (stack == null) continue;
                HItemStack hItemStack = plugin.getItemLibrary().get(stack);
                if (hItemStack.getItem() != item) continue;
                int stackAmount = stack.getAmount();
                if (stackAmount >= remaining) {
                    stack.setAmount(stackAmount - remaining);
                    break;
                } else {
                    remaining -= stackAmount;
                    stack.setAmount(0);
                }
            }
            onFinish(quester);
        });
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemID = cfg.getString("item");
        rawAmount = cfg.getString("amount", "1");
    }

}
