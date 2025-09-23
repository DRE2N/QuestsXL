package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class ExperienceObjective extends QBaseObjective<PlayerExpChangeEvent> {

    int amount;


    @Override
    public void check(ActiveObjective active, PlayerExpChangeEvent event) {

    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Experience; de=Erfahrung");
    }

    @Override
    public Class<PlayerExpChangeEvent> getEventType() {
        return PlayerExpChangeEvent.class;
    }
}
