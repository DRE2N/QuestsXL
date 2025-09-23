package de.erethon.questsxl.objective;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "jump",
        description = "This objective is completed when the player jumps.",
        shortExample = "jump:",
        longExample = {
                "jump:",
        }
)
public class JumpObjective extends QBaseObjective<PlayerJumpEvent> {

    @Override
    public void check(ActiveObjective active, PlayerJumpEvent e) {
        if (!conditions(e.getPlayer())) return;
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Jump!; de=Spring!");
    }

    @Override
    public Class<PlayerJumpEvent> getEventType() {
        return PlayerJumpEvent.class;
    }
}
