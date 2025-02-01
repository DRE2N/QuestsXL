package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QLoadableDoc;
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
    public boolean check(Quester quester) {
        if (quester instanceof QPlayer player) {
            if (player.getPlayer().isSneaking()) {
                return success(quester);
            }
        }
        return fail(quester);
    }
}
