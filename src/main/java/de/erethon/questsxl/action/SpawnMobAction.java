package de.erethon.questsxl.action;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.NPCData;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;

public class SpawnMobAction extends QBaseAction {

    Aether aether = plugin.getAether();
    CreatureManager creatureManager = aether.getCreatureManager();
    NPCData npcData = null;
    QLocation location = null;


    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        npcData.spawn(location.get(player.getPlayer().getLocation()));
        onFinish(player);
    }

    @Override
    public void play(QEvent event) {
        if (!conditions(event)) return;
        npcData.spawn(location.get(event.getLocation()));
        onFinish(event);
    }

    @Override
    public void load(QLineConfig cfg) {
        location = new QLocation(cfg);
        npcData = creatureManager.getByID(cfg.getString("id"));
        if (npcData == null) {
            throw new IllegalArgumentException("NPCData with id " + cfg.getString("id") + " not found.");
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        location = new QLocation(section);
        npcData = creatureManager.getByID(section.getString("id"));
        if (npcData == null) {
            throw new IllegalArgumentException("NPCData with id " + section.getString("id") + " not found.");
        }
    }
}
