package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "idle",
        description = "This condition is successful if the player has been idle for a certain amount of time.",
        shortExample = "idle: idle_duration=10",
        longExample = {
                "idle:",
                "  idle_duration: 10"
        }
)
public class IdleCondition extends QBaseCondition {

    @QParamDoc(description = "The duration in seconds the player has to be idle for, in second", required = true)
    private long idleDuration = 0;

    @Override
    public boolean check(Quester quester) {
        if (quester instanceof QPlayer player) {
            if (player.getPlayer().getIdleDuration().toSeconds() > idleDuration) {
                return success(quester);
            }
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        idleDuration = cfg.getLong("idle_duration", 0);
    }
}
