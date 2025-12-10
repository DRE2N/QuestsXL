package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityBreedEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@QLoadableDoc(
        value = "breed",
        description = "This objective is completed when the player breeds two entities.",
        shortExample = "breed: entity_type=pig,cow,sheep",
        longExample = {
                "breed:",
                "  entity_type: cow,pig,sheep"
        }
)
public class BreedObjective extends QBaseObjective<EntityBreedEvent> {

    @QParamDoc(description = "The entity type(s) that have to be bred (comma-separated for multiple types)", required = true)
    private final Set<EntityType> entityTypes = new HashSet<>();

    @Override
    public void check(ActiveObjective active, EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player player)) return;
        if (!entityTypes.isEmpty() && !entityTypes.contains(e.getMother().getType())) return;
        if (!conditions(player)) return;
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(player));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String entityTypeStr = cfg.getString("entity_type");
        if (entityTypeStr == null || entityTypeStr.trim().isEmpty()) {
            plugin.addRuntimeError(new FriendlyError(cfg.getName(), "Missing entity_type parameter", "No entity types specified", "Make sure to specify at least one entity type."));
            return;
        }

        String[] types = entityTypeStr.split(",");
        for (String type : types) {
            String trimmedType = type.trim().toUpperCase(Locale.ROOT);
            //noinspection UnstableApiUsage
            EntityType entityType = EntityType.fromName(trimmedType);
            if (entityType == null) {
                plugin.addRuntimeError(new FriendlyError(cfg.getName(), "Invalid entity type: " + trimmedType, "Unknown entity type", "Make sure the entity type is spelled correctly."));
            } else {
                entityTypes.add(entityType);
            }
        }
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Breed; de=Züchten");
    }

    @Override
    public Class<EntityBreedEvent> getEventType() {
        return EntityBreedEvent.class;
    }
}
