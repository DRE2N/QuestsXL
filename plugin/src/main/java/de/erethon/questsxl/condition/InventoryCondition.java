package de.erethon.questsxl.condition;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.hephaestus.items.HItemStack;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

@QLoadableDoc(
        value = "inventory_contains",
        description = "Checks if the player has the specified item in their inventory. Supports both Materials and Hephaestus items.",
        shortExample = "inventory_contains: material=diamond_ore; amount=5",
        longExample = {
                "inventory_contains:",
                "  item: 'erethon:hoe' # Needs to be quoted due to the colon",
                "  amount: 1",
        }
)
public class InventoryCondition extends QBaseCondition {

    @QParamDoc(name = "material", description = "The material of the item.")
    private Material material;
    @QParamDoc(name = "amount", description = "The amount of the item.")
    private int amount;
    @QParamDoc(name = "item", description = "The Hephaestus item to check for.")
    private HItem item;

    @Override
    public boolean check(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return fail(quester);
        }
        if (material != null) {
            if (player.getPlayer().getInventory().contains(material, amount)) {
                return success(player);
            }
        } else {
            if (player.getPlayer().getInventory().containsAtLeast(item.getBaseItem().getDefaultInstance().getBukkitStack(), amount)) {
                return success(player);
            }
        }
        return fail(player);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        if (cfg.contains("material")) {
            material = Material.getMaterial(cfg.getString("material", "AIR").toUpperCase(Locale.ROOT));
        }
        amount = cfg.getInt("amount", 1);
        if (cfg.contains("item")) {
            NamespacedKey key = NamespacedKey.fromString(cfg.getString("item", "minecraft:air"));
            item = QuestsXL.getInstance().getItemLibrary().get(key);
        }
    }
}
