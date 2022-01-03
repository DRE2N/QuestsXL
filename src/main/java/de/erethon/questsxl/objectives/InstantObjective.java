package de.erethon.questsxl.objectives;

import de.erethon.questsxl.events.QStageStartEvent;
import de.erethon.questsxl.quest.QStage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;

public class InstantObjective extends QBaseObjective {

    @Override
    public void check(Event e) {
        if (!(e instanceof QStageStartEvent event)) return;
        complete(event.getPlayer().getPlayer(), this);
    }

}
