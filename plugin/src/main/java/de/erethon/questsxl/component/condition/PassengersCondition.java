package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Map;

@QLoadableDoc(
        value = "passengers",
        description = "This condition is successful if the player has any passengers.",
        shortExample = "passengers:",
        longExample = {
                "passengers:",
        }
)
public class PassengersCondition extends QBaseCondition implements VariableProvider {

    private int lastPassengerCount = 0;

    @Override
    public boolean checkInternal(Quester quester) {
        if (quester instanceof QPlayer player) {
            int i = 0;
            for (Entity passenger : player.getPlayer().getPassengers()) {
                if (passenger.getType() != EntityType.BLOCK_DISPLAY || passenger.getType() != EntityType.ITEM_DISPLAY || passenger.getType() != EntityType.TEXT_DISPLAY) {
                    i++;
                }
            }
            lastPassengerCount = i;
            if (i > 0) {
                return success(quester);
            }
        }
        return fail(quester);
    }

    /** Exposes %passenger_count% to child actions (onSuccess / onFail). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("passenger_count", new QVariable(lastPassengerCount));
    }
}
