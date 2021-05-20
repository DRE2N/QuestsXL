package de.erethon.questsxl.condition;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.players.QPlayer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InvertedCondition extends QBaseCondition {

    Set<QCondition> conditions = new HashSet<>();

    @Override
    public boolean check(QPlayer player) {
        for (QCondition condition : conditions) {
            MessageUtil.log("Checking " + condition.getClass().getSimpleName() + ": " + condition.check(player));
            if (!condition.check(player)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        conditions = ConditionManager.loadConditions("InvertedCondition " + section.getName(), section.getConfigurationSection("conditions"));
    }

}
