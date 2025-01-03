package de.erethon.questsxl.action;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.NPCData;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;

@QLoadableDoc(
        value = "spawn_mob",
        description = "Spawns a mob at a location.",
        shortExample = "spawn_mob: mob=example_mob; x=5; y=0; z=0",
        longExample = {
                "spawn_mob:",
                "  mob: example_mob",
                "  location: ",
                "    x: ~40",
                "    y: ~0",
                "    z: ~5"
        }
)
public class SpawnMobAction extends QBaseAction {

    Aether aether = plugin.getAether();
    CreatureManager creatureManager = aether.getCreatureManager();

    @QParamDoc(name = "mob", description = "The ID of the mob to spawn", required = true)
    NPCData npcData = null;
    @QParamDoc(name = "location", description = "The location to spawn the mob at. QLocation", required = true)
    QLocation location = null;


    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        Location pLocation = player.getLocation();
        try {
            AetherBaseMob mob = npcData.spawn(location.get(pLocation));
            mob.setPos(location.getX(pLocation), location.getY(pLocation), location.getZ(pLocation));
            mob.addToWorld();
        }
        catch (Exception e) {
            FriendlyError error = new FriendlyError(id,"Failed to spawn mob", e.getMessage(), "Mob ID: " + npcData.getID()).addStacktrace(e.getStackTrace());
            plugin.addRuntimeError(error);
        }
        onFinish(player);
    }

    @Override
    public void play(QEvent event) {
        if (!conditions(event)) return;
        Location loc = event.getLocation();
        try {
            AetherBaseMob mob = npcData.spawn(location.get(loc));
            mob.setPos(location.getX(loc), location.getY(loc), location.getZ(loc));
            mob.addToWorld();
        }
        catch (Exception e) {
            FriendlyError error = new FriendlyError(id,"Failed to spawn mob", e.getMessage(), "Mob ID: " + npcData.getID()).addStacktrace(e.getStackTrace());
            plugin.addRuntimeError(error);
        }
        MessageUtil.log("Spawned " + npcData.getID() + " at " + location.get(event.getLocation()));
        onFinish(event);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        location = cfg.getQLocation("location");
        npcData = creatureManager.getByID(cfg.getString("mob"));
        if (npcData == null) { // Legacy support
            npcData = creatureManager.getByID(cfg.getString("id"));
        }
        if (npcData == null) {
            throw new IllegalArgumentException("NPCData with id " + cfg.getString("mob") + " not found.");
        }
    }
}
