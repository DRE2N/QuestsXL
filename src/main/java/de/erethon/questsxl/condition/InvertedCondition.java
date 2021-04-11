package de.erethon.questsxl.condition;

import de.erethon.questsxl.players.QPlayer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class InvertedCondition extends QBaseCondition {

    QCondition condition;

    @Override
    public boolean check(QPlayer player) {
        return !condition.check(player);
    }


    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
    }

}
