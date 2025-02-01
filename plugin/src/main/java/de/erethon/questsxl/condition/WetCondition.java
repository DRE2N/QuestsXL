package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "wet",
        description = "This condition is successful if the player is in water, rain or a bubble column.",
        shortExample = "wet:",
        longExample = {
                "wet:",
        }
)
public class WetCondition extends QBaseCondition {

    @Override
    public boolean check(Quester quester) {
        if (quester instanceof QPlayer player) {
            if (player.getPlayer().isInWaterOrRainOrBubbleColumn()) {
                return success(quester);
            }
        }
        return fail(quester);
    }
}
