package de.erethon.questsxl.condition;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class LevelCondition extends QBaseCondition {

    int level;

    @Override
    public boolean check(Quester quester) {
        return false; // missing player level check
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        level = cfg.getInt("level");
    }

}
