package de.erethon.questsxl.condition;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class LevelCondition extends QBaseCondition {

    int level;

    @Override
    public boolean check(QPlayer player) {
        return false; // missing player level check
    }

    @Override
    public boolean check(QEvent event) {
        return fail(event);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        level = section.getInt("level");
    }

    @Override
    public void load(QLineConfig section) {
        level = section.getInt("level");
    }
}
