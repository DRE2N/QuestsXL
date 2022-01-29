package de.erethon.questsxl.condition;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class PlayerScoreCondition extends QBaseCondition {

    String score;
    int value;

    @Override
    public boolean check(QPlayer player) {
        if (player.getScore(score) >= value) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public void load(String[] c) {
        score = c[0];
        value = NumberUtil.parseInt(c[1]);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        score = section.getString("score");
        value = section.getInt("value");
    }
}
