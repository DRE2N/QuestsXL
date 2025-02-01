package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Locale;

@QLoadableDoc(
        value = "mounted",
        description = "This condition is successful if the player is mounted on a vehicle.",
        shortExample = "mounted:",
        longExample = {
                "mounted:",
                "  entity_type: horse"
        }
)
public class MountedCondition extends QBaseCondition {

    @QParamDoc(name = "entity_type", description = "The type of entity the player is mounted on.")
    private EntityType entityType;

    @Override
    public boolean check(Quester quester) {
        if (quester instanceof QPlayer player) {
            Player p = player.getPlayer();
            if (p.isInsideVehicle()) {
                if (p.getVehicle() == null) {
                    return fail(quester);
                }
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

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        //noinspection UnstableApiUsage
        entityType = EntityType.fromName(cfg.getString("entity_type").toUpperCase(Locale.ROOT));
        if (entityType == null) {
            QuestsXL.getInstance().addRuntimeError(new FriendlyError(cfg.getName(), "Invalid entity type: " + cfg.getString("entity"), "Null entity type", "Make sure the entity type is spelled correctly."));
        }
    }
}
