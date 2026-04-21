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
        value = "minecraft_time",
        description = "Checks if the in-game time is within a specified range.",
        shortExample = "minecraft_time: minTime=0 maxTime=12000",
        longExample = {
                "minecraft_time:",
                "  minTime: 0",
                "  maxTime: 12000",
        }
)
public class MinecraftTimeCondition extends QBaseCondition implements VariableProvider {

    @QParamDoc(name = "minTime", description = "The minimum in-game time in ticks (0-24000).", required = true)
    private int minTime;
    @QParamDoc(name = "maxTime", description = "The maximum in-game time in ticks (0-24000).", required = true)
    private int maxTime;

    private long lastTime = 0;

    @Override
    public boolean checkInternal(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return fail(quester);
        }
        lastTime = player.getPlayer().getWorld().getTime();
        if (lastTime >= minTime && lastTime <= maxTime) {
            return success(quester);
        }
        return fail(quester);
    }

    /** Exposes %minecraft_time% to child actions (onSuccess / onFail). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("minecraft_time", new QVariable(lastTime));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        minTime = cfg.getInt("minTime", 0);
        maxTime = cfg.getInt("maxTime", 24000);
    }
}
