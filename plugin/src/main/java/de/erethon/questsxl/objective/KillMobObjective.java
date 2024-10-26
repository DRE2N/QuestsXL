package de.erethon.questsxl.objective;

import de.erethon.aether.creature.NPCData;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.InstancedCreatureDeathEvent;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

public class KillMobObjective extends QBaseObjective {

    private String mob;
    private int radius;

    @Override
    public void check(ActiveObjective active, Event e) {
        if (radius > 0) {
            if (active.getHolder() instanceof QEvent qEvent) {
                if (e instanceof CreatureDeathEvent event && event.getKiller().getLocation().distance(qEvent.getLocation()) <= radius) {
                    return;
                }
                if (e instanceof InstancedCreatureDeathEvent event && event.getKiller().getLocation().distance(qEvent.getLocation()) <= radius) {
                    return;
                }
                if (e instanceof EntityDeathEvent event && event.getEntity().getLocation().distance(qEvent.getLocation()) <= radius) {
                    return;
                }
            }
            return;
        }
        if (e instanceof CreatureDeathEvent event) {
            check(event.getNpc(), event.getKiller(), active);
        } else if (e instanceof InstancedCreatureDeathEvent event) {
            check(event.getNpc().getNpc(), event.getKiller(), active);
        }
        if (e instanceof EntityDeathEvent event) {
            if (event.getEntity().getType().name().equals(mob.toUpperCase()) && event.getEntity().getKiller() instanceof Player player) {
                checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(player));
            }
        }
    }

    private void check(NPCData npc, Player player, ActiveObjective active) {
        if (npc.getID().equals(mob)) {
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(player));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        mob = cfg.getString("id");
        radius = cfg.getInt("radius", -1);
    }
}
