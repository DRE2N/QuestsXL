package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;

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
    public boolean checkInternal(Quester quester) {
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
