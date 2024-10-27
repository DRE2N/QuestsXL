package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.event.QStageStartEvent;
import org.bukkit.event.Event;

@QLoadableDoc(
        value = "instant",
        description = "The opposite of the impossible objective. This objective is instantly completed when the stage starts.",
        shortExample = "instant:",
        longExample = {
                "instant:",
        }
)
public class InstantObjective extends QBaseObjective {

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof QStageStartEvent)) return;
        checkCompletion(active, this, null);
    }

}
