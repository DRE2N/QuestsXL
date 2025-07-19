package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerToggleSneakEvent;

@QLoadableDoc(
        value = "sneak",
        description = "This objective is completed when the player starts sneaking.",
        shortExample = "sneak:",
        longExample = {
                "sneak:",
        }
)
public class SneakObjective extends QBaseObjective<PlayerToggleSneakEvent> {

    @Override
    public void check(ActiveObjective active, PlayerToggleSneakEvent e) {
        if (!conditions(e.getPlayer())) return;
        if (!e.isSneaking()) return;
        checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(e.getPlayer()));
    }

    @Override
    public Class<PlayerToggleSneakEvent> getEventType() {
        return PlayerToggleSneakEvent.class;
    }
}
