package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.doc.QLoadableDoc;
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
    public boolean checkInternal(Quester quester) {
        if (quester.getLocation().getWorld().getWeatherDuration() > 0) {
            return success(quester);
        }
        return fail(quester);
    }
}
