package de.erethon.questsxl.condition;

import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class LevelCondition extends QBaseCondition {

    int level;

    @Override
    public boolean check(QPlayer player) {
        return false;
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
    }

}
