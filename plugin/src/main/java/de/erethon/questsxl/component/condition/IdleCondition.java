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
        value = "idle",
        description = "This condition is successful if the player has been idle for a certain amount of time.",
        shortExample = "idle: duration=10",
        longExample = {
                "idle:",
                "  duration: 10"
        }
)
public class IdleCondition extends QBaseCondition implements VariableProvider {

    @QParamDoc(name ="duration", description = "The duration in seconds the player has to be idle for, in second", required = true)
    private long idleDuration = 0;

    private long lastIdleSeconds = 0;

    @Override
    public boolean checkInternal(Quester quester) {
        if (quester instanceof QPlayer player) {
            lastIdleSeconds = player.getPlayer().getIdleDuration().toSeconds();
            if (lastIdleSeconds > idleDuration) {
                return success(quester);
            }
        }
        return fail(quester);
    }

    /** Exposes %idle_seconds% to child actions (onSuccess / onFail). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("idle_seconds", new QVariable(lastIdleSeconds));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        idleDuration = cfg.getLong("duration", 0);
    }
}
