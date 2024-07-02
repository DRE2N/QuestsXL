package de.erethon.questsxl.objective;

import de.erethon.aether.creature.NPCData;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.InstancedCreatureDeathEvent;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QLineConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

public class KillMobObjective extends QBaseObjective {

    String mob;
    int amount;
    int alreadyKilled = 0;

    @Override
    public void check(ActiveObjective active, Event e) {
        MessageUtil.log("Checking entity death event...");
        if (e instanceof CreatureDeathEvent event) {
            //check(event.getNpc().getNpc(), event.getKiller());
        } else if (e instanceof InstancedCreatureDeathEvent event) {
            //check(event.getNpc().getNpc(), event.getKiller());
        }
        if (e instanceof EntityDeathEvent event) {
            if (event.getEntity().getType().name().equals(mob.toUpperCase())) {
                alreadyKilled++;
                progress(plugin.getPlayerCache().getByPlayer(event.getEntity().getKiller()));
                MessageUtil.broadcastMessage("Killed mob. " + alreadyKilled + " / " + amount);
                if (alreadyKilled >= amount) {
                    complete(active.getHolder(), this);
                }
            }
        }
    }

    private void check(NPCData npc, Player killer) {
        if (npc.getID().equalsIgnoreCase(mob) && ++alreadyKilled >= amount) {
            complete(plugin.getPlayerCache().getByPlayer(killer), this);
        }
    }

    @Override
    public void onStart(ObjectiveHolder player) {
        alreadyKilled = 0;
    }

    @Override
    public void load(QLineConfig section) {
        mob = section.getString("id");
        amount = section.getInt("amount");
        if (amount <= 0) {
            throw new RuntimeException("The kill objective in " + section + " contains a negative amount.");
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        mob = section.getString("id");
        amount = section.getInt("amount");
        if (amount <= 0) {
            throw new RuntimeException("The kill objective in " + section.getName() + " contains a negative amount.");
        }
    }
}
