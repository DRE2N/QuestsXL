package de.erethon.questsxl.condition;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.hephaestus.items.HItemStack;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class InventoryCondition extends QBaseCondition {

    private Material material;
    private int amount;
    private HItem item;

    @Override
    public boolean check(QPlayer player) {
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
    public boolean check(QEvent event) {
        return fail(event);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        if (cfg.contains("material")) {
            material = Material.getMaterial(cfg.getString("material", "AIR"));
        }
        amount = cfg.getInt("amount", 1);
        if (cfg.contains("item")) {
            NamespacedKey key = NamespacedKey.fromString(cfg.getString("item", "minecraft:air"));
            item = QuestsXL.getInstance().getItemLibrary().get(key);
        }
    }
}
