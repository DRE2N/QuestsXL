package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerBedEnterEvent;

@QLoadableDoc(
        value = "sleep",
        description = "This objective is completed when the player sleeps. Can be cancelled, preventing the player from entering the bed.",
        shortExample = "sleep:",
        longExample = {
                "sleep:",
        }
)
public class SleepObjective extends QBaseObjective {

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof PlayerBedEnterEvent e)) return;
        if (!conditions(e.getPlayer())) return;
        if (shouldCancelEvent) e.setCancelled(true);
        checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(e.getPlayer()));
    }
}
