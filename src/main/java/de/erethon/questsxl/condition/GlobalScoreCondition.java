package de.erethon.questsxl.condition;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class GlobalScoreCondition extends QBaseCondition {

    String score;
    int value;

    @Override
    public boolean check(QPlayer player) {
        if (QuestsXL.getInstance().getScore(score) >= value) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        score = section.getString("score");
        value = section.getInt("value");
    }

    @Override
    public void load(String[] c) {
        score = c[0];
        value = NumberUtil.parseInt(c[1]);
    }
}
