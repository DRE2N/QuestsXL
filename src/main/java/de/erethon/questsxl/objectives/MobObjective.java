package de.erethon.questsxl.objectives;

import de.erethon.aether.creature.NPCData;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.InstancedCreatureDeathEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class MobObjective extends QBaseObjective {

    String mob;
    int amount;
    int alreadyKilled = 0;

    @Override
    public void check(Event e) {
        if (e instanceof CreatureDeathEvent event) {
            check(event.getNpc().getNpc(), event.getKiller());
        } else if (e instanceof InstancedCreatureDeathEvent event) {
            check(event.getNpc().getNpc(), event.getKiller());
        }
    }

    private void check(NPCData npc, Player killer) {
        if (npc.getID().equalsIgnoreCase(mob) && ++alreadyKilled >= amount) {
            complete(killer, this);
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
