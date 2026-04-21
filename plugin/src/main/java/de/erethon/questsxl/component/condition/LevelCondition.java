package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.player.QPlayer;

import java.util.Map;

@QLoadableDoc(
        value = "level",
        description = "This condition is successful if the player's experience level is within the specified range.",
        shortExample = "level: min=5; max=20",
        longExample = {
                "level:",
                "  min: 5",
                "  max: 20"
        }
)
public class LevelCondition extends QBaseCondition implements VariableProvider {

    @QParamDoc(name = "min", description = "The minimum level required.", def = "0")
    private int min;
    @QParamDoc(name = "max", description = "The maximum level allowed.", def = "Integer.MAX_VALUE")
    private int max;

    private int lastLevel = 0;

    @Override
    public boolean checkInternal(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return fail(quester);
        }
        lastLevel = player.getPlayer().getLevel();
        if (lastLevel >= min && lastLevel <= max) {
            return success(quester);
        }
        return fail(quester);
    }

    /** Exposes %level% to child actions (onSuccess / onFail). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("level", new QVariable(lastLevel));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        min = cfg.getInt("min", 0);
        max = cfg.getInt("max", Integer.MAX_VALUE);
    }
}
