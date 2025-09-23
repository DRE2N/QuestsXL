package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Player;
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
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Sneak; de=Schleichen");
    }

    @Override
    public Class<PlayerToggleSneakEvent> getEventType() {
        return PlayerToggleSneakEvent.class;
    }
}
