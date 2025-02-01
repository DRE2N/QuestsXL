package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.Quester;

@QLoadableDoc(
        value = "rain",
        description = "This condition is successful if it is raining.",
        shortExample = "rain:",
        longExample = {
                "rain:",
        }
)
public class RainCondition extends QBaseCondition {

    @Override
    public boolean check(Quester quester) {
        if (quester.getLocation().getWorld().getWeatherDuration() > 0) {
            return success(quester);
        }
        return fail(quester);
    }
}
