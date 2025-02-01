package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "health",
        description = "This condition is successful if the player's health is within the specified range.",
        shortExample = "health: min=10; max=20",
        longExample = {
                "health:",
                "  min: 10",
                "  max: 20"
        }
)
public class HealthCondition extends QBaseCondition {

    @QParamDoc(name = "min", description = "The minimum health value.", def = "0")
    private int minValue;
    @QParamDoc(name = "max", description = "The maximum health value.", def = "4096")
    private int maxValue;

    @Override
    public boolean check(Quester quester) {
        if (quester instanceof QPlayer player) {
            Player p = player.getPlayer();
            if (p.getHealth() >= minValue && p.getHealth() <= maxValue) {
                return success(quester);
            }
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        minValue = cfg.getInt("min", 0);
        maxValue = cfg.getInt("max", 4096);
    }
}
