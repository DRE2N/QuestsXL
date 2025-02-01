package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class ItemInHandCondition extends QBaseCondition {
    @Override
    public boolean check(Quester quester) {
        return false;
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
    }
}
