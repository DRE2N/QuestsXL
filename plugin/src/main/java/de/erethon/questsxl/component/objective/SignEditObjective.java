package de.erethon.questsxl.component.objective;

import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.script.QTranslatable;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "sign_edit",
        description = "This objective is completed when the player start editing a sign. Can be cancelled.",
        shortExample = "sign_edit:",
        longExample = {
                "sign_edit:",
        }
)
public class SignEditObjective extends QBaseObjective<PlayerOpenSignEvent> {

    @Override
    public void check(ActiveObjective active, PlayerOpenSignEvent e) {
        if (!conditions(e.getPlayer())) return;
        if (shouldCancelEvent) e.setCancelled(true);
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Edit sign; de=Bearbeite Schild");
    }

    @Override
    public Class<PlayerOpenSignEvent> getEventType() {
        return PlayerOpenSignEvent.class;
    }
}
