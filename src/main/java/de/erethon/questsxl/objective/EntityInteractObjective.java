package de.erethon.questsxl.objective;

import de.erethon.aether.creature.ActiveNPC;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class EntityInteractObjective extends QBaseObjective {

    String mob;

    @Override
    public void check(ActiveObjective active, Event e) {
        if (!(e instanceof PlayerInteractEntityEvent event)) {
            return;
        }
        Player player = event.getPlayer();
        if (!conditions(player)) {
            return;
        }
        Entity entity = event.getRightClicked();
        if (!entity.getType().name().equalsIgnoreCase(mob)) {
            /* Needs rework for new Aether
            ActiveNPC activeNPC = plugin.getAether().getActiveCreatureManager().get(entity.getUniqueId());
            if (activeNPC == null || !activeNPC.getNpc().getID().equalsIgnoreCase(mob)) {
                return;
            }*/
        }
        checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(player));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        mob = cfg.getString("id");
    }
}
