package de.erethon.questsxl.objective;

import de.erethon.questsxl.event.QStageStartEvent;
import org.bukkit.event.Event;

public class InstantObjective extends QBaseObjective {

    @Override
    public void check(Event e) {
        if (!(e instanceof QStageStartEvent event)) return;
        complete(event.getPlayer(), this);
    }

}
