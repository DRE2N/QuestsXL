package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

@QLoadableDoc(
        value = "death",
        description = "This objective is completed when the player dies.",
        shortExample = "death:",
        longExample = {
                "death:",
        }
)
public class DeathObjective extends QBaseObjective<PlayerDeathEvent> {

    @Override
    public void check(ActiveObjective active, PlayerDeathEvent e) {
        if (conditions(e.getEntity())) {
            if (shouldCancelEvent) e.setCancelled(true);
            checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getEntity()));
        }
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Die; de=Stirb");
    }

    @Override
    public Class<PlayerDeathEvent> getEventType() {
        return PlayerDeathEvent.class;
    }
}
