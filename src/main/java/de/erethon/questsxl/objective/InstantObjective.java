package de.erethon.questsxl.objective;

import de.erethon.questsxl.event.QStageStartEvent;
import org.bukkit.event.Event;

public class InstantObjective extends QBaseObjective {

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof QStageStartEvent)) return;
        complete(active.getHolder(), this);
    }

}
