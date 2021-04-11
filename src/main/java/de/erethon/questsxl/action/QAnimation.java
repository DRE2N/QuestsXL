package de.erethon.questsxl.action;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class QAnimation extends QBaseAction {

    String name;
    boolean instanced = true;

    @Override
    public void play(Player player) {
        if (!conditions(player)) return;
    }

    @Override
    public Material getIcon() {
        return Material.ITEM_FRAME;
    }

    @Override
    public void load(String[] msg) {
        super.load(msg);
        name = msg[0];
        instanced = Boolean.parseBoolean(msg[1]);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        name = section.getString("name", "none");
        instanced = section.getBoolean("instanced");
    }
}
