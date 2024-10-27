package de.erethon.questsxl.objective;

import de.erethon.aether.creature.NPCData;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.InstancedCreatureDeathEvent;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

@QLoadableDoc(
        value = "kill_mob",
        description = "This objective is completed when a player kills a specific mob, optionally within a certain radius around an event.",
        shortExample = "kill_mob: mob=evil_mob",
        longExample = {
                "kill_mob:",
                "  mob: 'evil_mob'",
                "  radius: 10"
        }
)
public class KillMobObjective extends QBaseObjective {

    @QParamDoc(name = "mob", description = "The ID of the mob", required = true)
    private String mob;
    @QParamDoc(name = "radius", description = "The radius around the event in which the mob has to be killed. Useful for events", def = "-1")
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
        mob = cfg.getString("mob");
        if (mob == null) { // Legacy
            mob = cfg.getString("id");
        }
        radius = cfg.getInt("radius", -1);
    }
}
