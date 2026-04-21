package de.erethon.questsxl.component.objective;

import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;

@QLoadableDoc(
        value = "kill_player",
        description = "This objective is completed by killing another player.",
        shortExample = "kill_player:",
        longExample = {
                "kill_player:",
        }
)
public class KillPlayerObjective extends QBaseObjective<PlayerDeathEvent> implements VariableProvider {

    private int lastProgress = 0;

    @Override
    public void check(ActiveObjective active, PlayerDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null || !conditions(killer)) {
            return;
        }
        lastProgress = active.getProgress() + 1;
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));
    }

    /** Exposes %progress% and %goal% to child actions (onComplete / onProgress). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of(
                "progress", new QVariable(lastProgress),
                "goal", new QVariable(progressGoal)
        );
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
