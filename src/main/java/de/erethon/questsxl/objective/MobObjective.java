package de.erethon.questsxl.objective;

import de.erethon.aether.creature.NPCData;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.InstancedCreatureDeathEvent;
import de.erethon.bedrock.chat.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Arrays;

public class MobObjective extends QBaseObjective {

    String mob;
    int amount;
    int alreadyKilled = 0;

    @Override
    public void check(Event e) {
        MessageUtil.log("Checking entity death event...");
        if (e instanceof CreatureDeathEvent event) {
            //check(event.getNpc().getNpc(), event.getKiller());
        } else if (e instanceof InstancedCreatureDeathEvent event) {
            //check(event.getNpc().getNpc(), event.getKiller());
        }
        if (e instanceof EntityDeathEvent event) {
            if (event.getEntity().getType().name().equals(mob.toUpperCase())) {
                alreadyKilled++;
                MessageUtil.log("Killed mob. " + alreadyKilled + " / " + amount);
                if (alreadyKilled >= amount) {
                    complete(plugin.getPlayerCache().getByPlayer(event.getEntity().getKiller()), this);
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
    public void load(String[] c) {
        super.load(c);
        mob = c[0];
        amount = Integer.parseInt(c[1]);
        if (amount <= 0) {
            throw new RuntimeException("The kill player objective in " + Arrays.toString(c) + " contains a negative amount.");
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        mob = section.getString("mob");
        amount = section.getInt("amount");
        if (amount <= 0) {
            throw new RuntimeException("The kill player objective in " + section.getName() + " contains a negative amount.");
        }
    }
}
