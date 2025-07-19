package de.erethon.questsxl.objective;

import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;

public class TakeDamageObjective extends QBaseObjective<EntityDamageEvent> {

    @Override
    public void check(ActiveObjective active, EntityDamageEvent event) {

    }

    @Override
    public Class<EntityDamageEvent> getEventType() {
        return EntityDamageEvent.class;
    }
}
