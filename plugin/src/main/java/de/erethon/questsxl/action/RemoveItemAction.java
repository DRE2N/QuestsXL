package de.erethon.questsxl.action;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemStack;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

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
    @QParamDoc(name = "amount", description = "The amount of items to remove", def="1")
    private int amount = 1;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        if (!(quester instanceof QPlayer qplayer)) {
            return;
        }
        execute(quester, (QPlayer player) -> {
            NamespacedKey key = NamespacedKey.fromString(itemID);
            HItem item = plugin.getItemLibrary().get(key);
            if (item == null) {
                return;
            }
            for (ItemStack stack : qplayer.getPlayer().getInventory().getContents()) {
                if (stack == null) continue;
                HItemStack hItemStack = plugin.getItemLibrary().get(stack);
                if (hItemStack.getItem() != item) continue;
                int stackAmount = stack.getAmount();
                if (stackAmount >= amount) {
                    stack.setAmount(stackAmount - amount);
                    break;
                } else {
                    amount -= stackAmount;
                    stack.setAmount(0);
                }
            }
            onFinish(quester);
        });
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemID = cfg.getString("item");
        amount = cfg.getInt("amount", 1);
    }

}
