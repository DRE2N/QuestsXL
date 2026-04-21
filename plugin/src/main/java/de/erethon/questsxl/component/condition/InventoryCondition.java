package de.erethon.questsxl.component.condition;

import de.erethon.hephaestus.items.HItem;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

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
public class InventoryCondition extends QBaseCondition implements VariableProvider {

    @QParamDoc(name = "material", description = "The material of the item.")
    private Material material;
    @QParamDoc(name = "amount", description = "The amount of the item.")
    private int amount;
    @QParamDoc(name = "item", description = "The Hephaestus item to check for.")
    private HItem item;

    private int lastItemCount = 0;

    @Override
    public boolean checkInternal(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return fail(quester);
        }
        if (material != null) {
            lastItemCount = Arrays.stream(player.getPlayer().getInventory().getContents())
                    .filter(s -> s != null && s.getType() == material)
                    .mapToInt(ItemStack::getAmount)
                    .sum();
            if (lastItemCount >= amount) {
                return success(player);
            }
        } else {
            ItemStack base = item.getBaseItem().getDefaultInstance().getBukkitStack();
            lastItemCount = Arrays.stream(player.getPlayer().getInventory().getContents())
                    .filter(s -> s != null && s.isSimilar(base))
                    .mapToInt(ItemStack::getAmount)
                    .sum();
            if (lastItemCount >= amount) {
                return success(player);
            }
        }
        return fail(player);
    }

    /** Exposes %item_count% (how many of the item the player actually has) to child actions. */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("item_count", new QVariable(lastItemCount));
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
            item = QuestsXL.get().getItemLibrary().get(key);
        }
    }
}
