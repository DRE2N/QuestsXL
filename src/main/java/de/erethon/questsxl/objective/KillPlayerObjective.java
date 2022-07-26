package de.erethon.questsxl.objective;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;

public class KillPlayerObjective extends QBaseObjective {

    String player;
    int amount;
    int alreadyKilled = 0;

    @Override
    public void check(Event e) {
        if (!(e instanceof PlayerDeathEvent event)) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null || !killer.getName().equalsIgnoreCase(player) || !conditions(killer)) {
            return;
        }
        if (++alreadyKilled >= amount) {
            complete(plugin.getPlayerCache().getByPlayer(killer), this);
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        amount = section.getInt("amount");
        if (amount <= 0) {
            throw new RuntimeException("The kill player objective in " + section.getName() + " contains a negative amount.");
        }
    }
}
