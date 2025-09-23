package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityBreedEvent;

public class FeedMobObjective extends QBaseObjective<EntityBreedEvent> {
    @Override
    public void check(ActiveObjective active, EntityBreedEvent event) {

    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Feed mob; de=Füttere Mob");
    }

    @Override
    public Class<EntityBreedEvent> getEventType() {
        return EntityBreedEvent.class;
    }
}
