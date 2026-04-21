package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "freezing",
        description = "This condition is successful if the player is frozen, e.g. in powder snow.",
        shortExample = "freezing:",
        longExample = {
                "freezing:",
        }
)
public class FreezingCondition extends QBaseCondition {

    @Override
    public boolean checkInternal(Quester quester) {
        if (quester instanceof QPlayer player) {
            if (player.getPlayer().getFreezeTicks() > 0) {
                return success(quester);
            }
        }
        return fail(quester);
    }
}
