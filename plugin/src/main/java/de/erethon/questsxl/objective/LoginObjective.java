package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;

@QLoadableDoc(
        value = "login",
        description = "This objective is completed when the player logs in.",
        shortExample = "login:",
        longExample = {
                "login:",
        }
)
public class LoginObjective extends QBaseObjective {

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof PlayerJoinEvent e)) return;
        if (!conditions(e.getPlayer())) return;
        checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(e.getPlayer()));
    }
}
