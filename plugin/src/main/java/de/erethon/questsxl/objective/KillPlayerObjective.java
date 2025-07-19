package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;

public class KillPlayerObjective extends QBaseObjective<PlayerDeathEvent> {

    @Override
    public void check(ActiveObjective active, PlayerDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null|| !conditions(killer)) {
            return;
        }

        checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(e.getPlayer()));

    }

    @Override
    public Class<PlayerDeathEvent> getEventType() {
        return PlayerDeathEvent.class;
    }
}
