package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "fire",
        description = "This condition is successful if the player is on fire.",
        shortExample = "fire:",
        longExample = {
                "fire:",
        }
)
public class FireCondition extends QBaseCondition {

    @Override
    public boolean checkInternal(Quester quester) {
        if (quester instanceof QPlayer player) {
            if (player.getPlayer().getFireTicks() > 0) {
                return success(quester);
            }
        }
        return fail(quester);
    }
}
