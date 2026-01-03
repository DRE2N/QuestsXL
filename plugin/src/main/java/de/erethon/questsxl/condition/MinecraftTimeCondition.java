package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;

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
public class MinecraftTimeCondition extends QBaseCondition {

    @QParamDoc(name = "minTime", description = "The minimum in-game time in ticks (0-24000).", required = true)
    private int minTime;
    @QParamDoc(name = "maxTime", description = "The maximum in-game time in ticks (0-24000).", required = true)
    private int maxTime;

    @Override
    public boolean check(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return fail(quester);
        }
        long time = player.getPlayer().getWorld().getTime();
        if (time >= minTime && time <= maxTime) {
            return success(quester);
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        minTime = cfg.getInt("minTime", 0);
        maxTime = cfg.getInt("maxTime", 24000);
    }
}
