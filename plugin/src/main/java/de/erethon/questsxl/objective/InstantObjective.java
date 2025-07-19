package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.event.QStageStartEvent;

@QLoadableDoc(
        value = "instant",
        description = "The opposite of the impossible objective. This objective is instantly completed when the stage starts.",
        shortExample = "instant:",
        longExample = {
                "instant:",
        }
)
public class InstantObjective extends QBaseObjective<QStageStartEvent> {

    @Override
    public void check(ActiveObjective active, QStageStartEvent e) {
        if (!e.getStage().hasObjective(this)) return;
        if (!conditions(e.getPlayer().getPlayer())) return;
        checkCompletion(active, this, null);
    }

    @Override
    public Class<QStageStartEvent> getEventType() {
        return QStageStartEvent.class;
    }

}
