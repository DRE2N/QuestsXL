package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import org.bukkit.event.Event;

@QLoadableDoc(
        value = "sign_edit",
        description = "This objective is completed when the player start editing a sign. Can be cancelled.",
        shortExample = "sign_edit:",
        longExample = {
                "sign_edit:",
        }
)
public class SignEditObjective extends QBaseObjective<PlayerOpenSignEvent> {

    @Override
    public void check(ActiveObjective active, PlayerOpenSignEvent e) {
        if (!conditions(e.getPlayer())) return;
        if (shouldCancelEvent) e.setCancelled(true);
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));
    }

    @Override
    public Class<PlayerOpenSignEvent> getEventType() {
        return PlayerOpenSignEvent.class;
    }
}
