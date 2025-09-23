package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class TakeDamageObjective extends QBaseObjective<EntityDamageEvent> {

    @Override
    public void check(ActiveObjective active, EntityDamageEvent event) {

    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Take damage; de=Schaden nehmen");
    }

    @Override
    public Class<EntityDamageEvent> getEventType() {
        return EntityDamageEvent.class;
    }
}
