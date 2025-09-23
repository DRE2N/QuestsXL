package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerBedEnterEvent;

@QLoadableDoc(
        value = "sleep",
        description = "This objective is completed when the player sleeps. Can be cancelled, preventing the player from entering the bed.",
        shortExample = "sleep:",
        longExample = {
                "sleep:",
        }
)
public class SleepObjective extends QBaseObjective<PlayerBedEnterEvent> {

    @Override
    public void check(ActiveObjective active, PlayerBedEnterEvent e) {
        if (!conditions(e.getPlayer())) return;
        if (shouldCancelEvent) e.setCancelled(true);
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Sleep; de=Schlafe");
    }

    @Override
    public Class<PlayerBedEnterEvent> getEventType() {
        return PlayerBedEnterEvent.class;
    }
}
