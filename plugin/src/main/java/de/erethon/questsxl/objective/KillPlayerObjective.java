package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

@QLoadableDoc(
        value = "kill_player",
        description = "This objective is completed by killing another player.",
        shortExample = "kill_player:",
        longExample = {
                "kill_player:",
        }
)
public class KillPlayerObjective extends QBaseObjective<PlayerDeathEvent> {


    @Override
    public void check(ActiveObjective active, PlayerDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null|| !conditions(killer)) {
            return;
        }

        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));

    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        if (progressGoal > 1) {
            return QTranslatable.fromString("en=Kill " + progressGoal + " players; de=Töte " + progressGoal + " Spieler");
        }
        return QTranslatable.fromString("en=Kill a player; de=Töte einen Spieler");
    }

    @Override
    public Class<PlayerDeathEvent> getEventType() {
        return PlayerDeathEvent.class;
    }
}
