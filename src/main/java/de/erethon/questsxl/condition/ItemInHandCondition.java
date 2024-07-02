package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class ItemInHandCondition extends QBaseCondition {
    @Override
    public boolean check(QPlayer player) {
        return false;
    }

    @Override
    public boolean check(QEvent event) {
        return false;
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
    }

    @Override
    public void load(QLineConfig section) {
        super.load(section);
    }
}
