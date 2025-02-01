package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityBreedEvent;

import java.util.Locale;

@QLoadableDoc(
        value = "breed",
        description = "This objective is completed when the player breeds two entities.",
        shortExample = "breed: entity_type=pig",
        longExample = {
                "breed:",
                "  entity_type: cow"
        }
)
public class BreedObjective extends QBaseObjective {

    @QParamDoc(description = "The entity type that has to be bred", required = true)
    private EntityType entityType;

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof EntityBreedEvent e)) return;
        if (!(e.getBreeder() instanceof Player player)) return;
        if (entityType != null && e.getMother().getType() != entityType) return;
        if (!conditions(player)) return;
        checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(player));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        //noinspection UnstableApiUsage
        entityType = EntityType.fromName(cfg.getString("entity_type").toUpperCase(Locale.ROOT));
        if (entityType == null) {
            plugin.addRuntimeError(new FriendlyError(cfg.getName(), "Invalid entity type: " + cfg.getString("entity"), "Null entity type", "Make sure the entity type is spelled correctly."));
        }
    }
}
