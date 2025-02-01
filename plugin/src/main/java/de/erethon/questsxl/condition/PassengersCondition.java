package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

@QLoadableDoc(
        value = "passengers",
        description = "This condition is successful if the player has any passengers.",
        shortExample = "passengers:",
        longExample = {
                "passengers:",
        }
)
public class PassengersCondition extends QBaseCondition {

    @Override
    public boolean check(Quester quester) {
        if (quester instanceof QPlayer player) {
            int i = 0;
            for (Entity passenger : player.getPlayer().getPassengers()) {
                if (passenger.getType() != EntityType.BLOCK_DISPLAY || passenger.getType() != EntityType.ITEM_DISPLAY || passenger.getType() != EntityType.TEXT_DISPLAY) {
                    i++;
                }
            }
            if (i > 0) {
                return success(quester);
            }
        }
        return fail(quester);
    }
}
