package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.players.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class GlobalScoreCondition extends QBaseCondition {

    String score;
    int value;

    @Override
    public boolean check(QPlayer player) {
        return QuestsXL.getInstance().getScore(score) >= value;
    }

    @Override
    public void load(String[] c) {
        score = c[0];
        value = Integer.parseInt(c[1]);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        score = section.getString("score");
        value = section.getInt("value");
    }
}
