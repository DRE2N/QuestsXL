package de.erethon.questsxl.component.objective;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityBreedEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
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
public class BreedObjective extends QBaseObjective<EntityBreedEvent> implements VariableProvider {

    @QParamDoc(description = "The entity type(s) that have to be bred (comma-separated for multiple types)", required = true)
    private final Set<EntityType> entityTypes = new HashSet<>();

    private int lastProgress = 0;
    private String lastEntityType = "";

    @Override
    public void check(ActiveObjective active, EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player player)) return;
        if (!entityTypes.isEmpty() && !entityTypes.contains(e.getMother().getType())) return;
        if (!conditions(player)) return;
        lastProgress = active.getProgress() + 1;
        lastEntityType = e.getMother().getType().name();
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(player));
    }

    /** Exposes %progress%, %goal%, %entity_type% to child actions (onComplete / onProgress). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of(
                "progress", new QVariable(lastProgress),
                "goal", new QVariable(progressGoal),
                "entity_type", new QVariable(lastEntityType)
        );
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
