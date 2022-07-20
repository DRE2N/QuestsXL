package de.erethon.questsxl.condition;

import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class InventoryCondition extends QBaseCondition {

    Material material;
    int amount;
    ItemStack itemStack;

    @Override
    public boolean check(QPlayer player) {
        if (material != null) {
            if (player.getPlayer().getInventory().contains(material, amount)) {
                return success(player);
            }
        } else {
            if (player.getPlayer().getInventory().containsAtLeast(itemStack, amount)) {
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
    public void load(ConfigurationSection section) {
        super.load(section);
    }
}
