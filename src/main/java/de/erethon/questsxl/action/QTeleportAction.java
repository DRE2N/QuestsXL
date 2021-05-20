package de.erethon.questsxl.action;

import de.erethon.commons.chat.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class QTeleportAction extends QBaseAction{

    Location target;

    public void play(Player player) {
        if (!conditions(player)) return;
        player.teleport(target);
        onFinish(player);
    }

    public Material getIcon() {
        return Material.ENDER_PEARL;
    }

    @Override
    public void load(String[] msg) {
        World world = Bukkit.getWorld(msg[0]);
        double x = Double.parseDouble(msg[1]);
        double y = Double.parseDouble(msg[2]);
        double z = Double.parseDouble(msg[3]);
        if (world == null) {
            throw new RuntimeException("The action " + Arrays.toString(msg) + " contains a location in an invalid world.");
        }
        target = new Location(world, x, y, z);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0.00);
        float pitch = (float) section.getDouble("pitch", 0.00);
        if (world == null) {
            throw new RuntimeException("The action " + id + " contains a location in an invalid world.");
        }
        target = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }
}
