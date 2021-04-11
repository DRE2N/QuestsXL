package de.erethon.questsxl.condition;

import de.erethon.questsxl.players.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class GlobalVariableCondition extends QBaseCondition {

    @Override
    public boolean check(QPlayer player) {
        return false;
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
    }
}
