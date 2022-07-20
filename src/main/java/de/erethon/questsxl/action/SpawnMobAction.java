package de.erethon.questsxl.action;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.NPCData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class SpawnMobAction extends QBaseAction {

    Aether aether = plugin.getAether();
    CreatureManager creatureManager = aether.getCreatureManager();
    NPCData npcData = null;
    Location location = null;


    @Override
    public void play(Player player) {
        if (!conditions(player)) return;
        ActiveNPC activeNPC = new ActiveNPC(npcData);
        activeNPC.spawn(location);
        onFinish(player);
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
        location = new Location(world, x, y, z);
        npcData = creatureManager.getByID(msg[4]);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        if (world == null) {
            throw new RuntimeException("The action " + id + " contains a location in an invalid world.");
        }
        location = new Location(Bukkit.getWorld(world), x, y, z);
        npcData = creatureManager.getByID(section.getString("id"));
    }
}
