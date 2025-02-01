package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;

@QLoadableDoc(
        value = "death",
        description = "This objective is completed when the player dies.",
        shortExample = "death:",
        longExample = {
                "death:",
        }
)
public class DeathObjective extends QBaseObjective {

    @Override
    public void check(ActiveObjective active, Event event) {
        if (event instanceof PlayerDeathEvent e) {
            if (conditions(e.getEntity())) {
                if (shouldCancelEvent) e.setCancelled(true);
                checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(e.getEntity()));
            }
        }
    }

}
