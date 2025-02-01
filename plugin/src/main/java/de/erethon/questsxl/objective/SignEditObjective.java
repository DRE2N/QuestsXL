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
public class SignEditObjective extends QBaseObjective {

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof PlayerOpenSignEvent e)) return;
        if (!conditions(e.getPlayer())) return;
        if (shouldCancelEvent) e.setCancelled(true);
        checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(e.getPlayer()));
    }
}
