package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

@QLoadableDoc(
        value = "mounted",
        description = "This condition is successful if the player is mounted on a vehicle.",
        shortExample = "mounted:",
        longExample = {
                "mounted:",
                "  entity_type: horse"
        }
)
public class MountedCondition extends QBaseCondition implements VariableProvider {

    @QParamDoc(name = "entity_type", description = "The type of entity the player is mounted on.")
    private EntityType entityType;

    private String lastMountType = "";

    @Override
    public boolean checkInternal(Quester quester) {
        if (quester instanceof QPlayer player) {
            Player p = player.getPlayer();
            if (p.isInsideVehicle()) {
                if (p.getVehicle() == null) {
                    return fail(quester);
                }
                lastMountType = p.getVehicle().getType().name().toLowerCase(Locale.ROOT);
                if (entityType == null) {
                    return success(quester);
                }
                if (p.getVehicle().getType() == entityType) {
                    return success(quester);
                }
            }
        }
        return fail(quester);
    }

    /** Exposes %mount_type% (entity type name of the vehicle) to child actions. */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("mount_type", new QVariable(lastMountType));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        //noinspection UnstableApiUsage
        entityType = EntityType.fromName(cfg.getString("entity_type").toUpperCase(Locale.ROOT));
        if (entityType == null) {
            QuestsXL.get().addRuntimeError(new FriendlyError(cfg.getName(), "Invalid entity type: " + cfg.getString("entity"), "Null entity type", "Make sure the entity type is spelled correctly."));
        }
    }
}
