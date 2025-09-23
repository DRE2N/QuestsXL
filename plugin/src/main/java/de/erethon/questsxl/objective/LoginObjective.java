package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

@QLoadableDoc(
        value = "login",
        description = "This objective is completed when the player logs in.",
        shortExample = "login:",
        longExample = {
                "login:",
        }
)
public class LoginObjective extends QBaseObjective<PlayerJoinEvent> {

    @Override
    public void check(ActiveObjective active, PlayerJoinEvent event) {
        if (!conditions(event.getPlayer())) return;
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(event.getPlayer()));
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Login; de=Einloggen");
    }

    @Override
    public Class<PlayerJoinEvent> getEventType() {
        return PlayerJoinEvent.class;
    }
}
