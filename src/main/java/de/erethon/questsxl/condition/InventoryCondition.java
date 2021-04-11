package de.erethon.questsxl.condition;

import de.erethon.questsxl.players.QPlayer;
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
            return player.getPlayer().getInventory().contains(material, amount);
        } else {
            return player.getPlayer().getInventory().containsAtLeast(itemStack, amount);
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
    }
}
