package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "sprinting",
        description = "This condition is successful if the player is sprinting.",
        shortExample = "sprinting:",
        longExample = {
                "sprinting:",
        }
)
public class SprintingCondition extends QBaseCondition {

    @Override
    public boolean check(Quester quester) {
        if (quester instanceof QPlayer player) {
            if (player.getPlayer().isSprinting()) {
                return success(quester);
            }
        }
        return fail(quester);
    }
}
