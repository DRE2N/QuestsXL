package de.erethon.questsxl.action;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemStack;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

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
    @QParamDoc(name = "amount", description = "The amount of items to give", def="1")
    private int amount = 1;
    @QParamDoc(name = "chance", description = "The chance of the item being given. 100 is 100% chance, 0 is 0% chance", def="100")
    private int chance = 100;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
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

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemID = cfg.getString("item");
        amount = cfg.getInt("amount", 1);
        chance = cfg.getInt("chance", 100);
    }

}
