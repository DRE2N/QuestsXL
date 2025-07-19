package de.erethon.questsxl.objective;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class ExperienceObjective extends QBaseObjective<PlayerExpChangeEvent> {

    int amount;


    @Override
    public void check(ActiveObjective active, PlayerExpChangeEvent event) {

    }

    @Override
    public Class<PlayerExpChangeEvent> getEventType() {
        return PlayerExpChangeEvent.class;
    }
}
