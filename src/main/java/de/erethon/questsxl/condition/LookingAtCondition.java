package de.erethon.questsxl.condition;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.players.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class LookingAtCondition extends QBaseCondition {

    Location target;
    Material block;

    @Override
    public boolean check(QPlayer player) {
        return false;
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        if (section.contains("block")) {
            block = Material.valueOf(section.getString("block"));
            return;
        }
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        if (world == null) {
            MessageUtil.log("The condition " + section.getName() + " contains a teleport for a world that is not loaded.");
            return;
        }
        target = new Location(Bukkit.getWorld(world), x, y, z);
    }

    @Override
    public void load(String[] msg) {

    }
}
