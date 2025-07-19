package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import org.bukkit.event.Event;

@QLoadableDoc(
        value = "impossible",
        description = "This objective is impossible to complete. It can be used to block progression, for example to only manually progress a stage with the set_stage action",
        shortExample = "impossible:",
        longExample = {
                "impossible:",
        }
)
public class ImpossibleObjective extends QBaseObjective {

    @Override
    public void check(ActiveObjective active, Event event) {

    }

    @Override
    public Class<?> getEventType() {
        return null;
    }
}
