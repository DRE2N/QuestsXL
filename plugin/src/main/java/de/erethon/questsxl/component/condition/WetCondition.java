package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.doc.QLoadableDoc;
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
    public boolean checkInternal(Quester quester) {
        if (quester instanceof QPlayer player) {
            if (player.getPlayer().isInWater() || player.getPlayer().isInRain()) {
                return success(quester);
            }
        }
        return fail(quester);
    }
}
