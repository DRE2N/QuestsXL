package de.erethon.questsxl.objective;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import de.erethon.questsxl.common.QLoadableDoc;
import org.bukkit.event.Event;

@QLoadableDoc(
        value = "jump",
        description = "This objective is completed when the player jumps.",
        shortExample = "jump:",
        longExample = {
                "jump:",
        }
)
public class JumpObjective extends QBaseObjective {

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof PlayerJumpEvent e)) return;
        if (!conditions(e.getPlayer())) return;
        checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(e.getPlayer()));
    }
}
