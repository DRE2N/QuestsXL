package de.erethon.questsxl.objective;

import org.bukkit.event.entity.EntityBreedEvent;

public class FeedMobObjective extends QBaseObjective<EntityBreedEvent> {
    @Override
    public void check(ActiveObjective active, EntityBreedEvent event) {

    }

    @Override
    public Class<EntityBreedEvent> getEventType() {
        return EntityBreedEvent.class;
    }
}
