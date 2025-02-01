package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "permission",
        description = "Checks if the player has a certain permission.",
        shortExample = "permission: permission=questsxl.test",
        longExample = {
                "permission:",
                "  permission: questsxl.test",
        }
)
public class PermissionCondition extends QBaseCondition {

    @QParamDoc(name = "permission", description = "The permission the player has to have.", required = true)
    String permission;

    @Override
    public boolean check(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return check(quester);
        }
        if (player.getPlayer().hasPermission(permission)) {
            return success(player);
        }
        return fail(player);
    }


    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        permission = cfg.getString("permission");
    }

}
