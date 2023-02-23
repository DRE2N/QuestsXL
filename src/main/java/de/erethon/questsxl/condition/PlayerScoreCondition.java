package de.erethon.questsxl.condition;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
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
    public boolean check(QEvent event) {
        if (event.getScore(score) >= value) {
            return success(event);
        }
        return fail(event);
    }

    @Override
    public void load(QLineConfig section) {
        score = section.getString("score");
        value = section.getInt("value");
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        score = section.getString("score");
        value = section.getInt("value");
    }
}
