package de.erethon.questsxl.objective;

import de.erethon.aether.creature.ActiveNPC;
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
            ActiveNPC activeNPC = plugin.getAether().getActiveCreatureManager().get(entity.getUniqueId());
            if (activeNPC == null || !activeNPC.getNpc().getID().equalsIgnoreCase(mob)) {
                return;
            }
        }
        complete(active.getHolder(), this);
    }

    @Override
    public void load(QLineConfig section) {
        mob = section.getString("id");
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        mob = section.getString("id");
    }
}
