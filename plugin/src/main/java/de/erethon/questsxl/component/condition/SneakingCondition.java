package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "sneaking",
        description = "This condition is successful if the player is sneaking.",
        shortExample = "sneaking:",
        longExample = {
                "sneaking:",
        }
)
public class SneakingCondition extends QBaseCondition {

    @Override
    public boolean checkInternal(Quester quester) {
        if (quester instanceof QPlayer player) {
            if (player.getPlayer().isSneaking()) {
                return success(quester);
            }
        }
        return fail(quester);
    }
}
