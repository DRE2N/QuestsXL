package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;

public class KillPlayerObjective extends QBaseObjective {
    String player;

    @Override
    public void check(ActiveObjective active, Event e) {
        if (!(e instanceof PlayerDeathEvent event)) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null || !killer.getName().equalsIgnoreCase(player) || !conditions(killer)) {
            return;
        }

        checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(event.getPlayer()));

    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        player = cfg.getString("player");
    }
}
