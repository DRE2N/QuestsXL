package de.erethon.questsxl.action;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemStack;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;

public class GiveItemAction extends QBaseAction {

    private String itemID;
    private int amount = 1;
    private int chance = 100;

    @Override
    public void play(QPlayer player) {
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
        stack.getVanillaStack().setCount(amount);
        player.getPlayer().getInventory().addItem(stack.getBukkitStack());
    }

    @Override
    public void play(QEvent event) {
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
        stack.getVanillaStack().setCount(amount);
        for (QPlayer player : event.getPlayersInRange()) {
            player.getPlayer().getInventory().addItem(stack.getBukkitStack());
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemID = cfg.getString("item");
        amount = cfg.getInt("amount", 1);
        chance = cfg.getInt("chance", 100);
    }

}
