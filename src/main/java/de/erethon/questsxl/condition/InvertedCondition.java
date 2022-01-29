package de.erethon.questsxl.condition;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
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
