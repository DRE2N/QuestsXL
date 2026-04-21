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

import java.util.Random;

@QLoadableDoc(
        value = "give_item",
        description = "Give an item to the player. The item needs to be defined in the Hephaestus item library. <br>You can check if an item exists by using `/give` in Minecraft.",
        shortExample = "give_item: item=minecraft:stone; amount=420; chance=69",
        longExample = {
                "give_item:",
                "  item: 'minecraft:stone' # The ID needs to be quoted due to the colon",
                "  amount: 420",
                "  chance: 69"
        }
)
public class GiveItemAction extends QBaseAction {

    @QParamDoc(name = "item", description = "The ID of the item to give", required = true)
    private String itemID;
    @QParamDoc(name = "amount", description = "The amount of items to give — supports %variables%", def="1")
    private String rawAmount = "1";
    @QParamDoc(name = "chance", description = "The chance of the item being given (0-100) — supports %variables%", def="100")
    private String rawChance = "100";

    @Override
    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        ExecutionContext ctx = ExecutionContext.current();
        int amount = ctx != null ? ctx.resolveInt(rawAmount) : parseInt(rawAmount);
        int chance = ctx != null ? ctx.resolveInt(rawChance) : parseInt(rawChance);
        Random random = new Random();
        if (random.nextInt(100) > chance) {
            return;
        }
        NamespacedKey key = NamespacedKey.fromString(itemID);
        HItem item = plugin.getItemLibrary().get(key);
        if (item == null) {
            return;
        }
        HItemStack stack = item.rollRandomStack();
        stack.getBukkitStack().setAmount(amount);
        execute(quester, (QPlayer player) -> player.getPlayer().getInventory().addItem(stack.getBukkitStack()));
        onFinish(quester);
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemID = cfg.getString("item");
        rawAmount = cfg.getString("amount", "1");
        rawChance = cfg.getString("chance", "100");
    }

}
