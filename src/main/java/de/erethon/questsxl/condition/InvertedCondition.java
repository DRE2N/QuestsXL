package de.erethon.questsxl.condition;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public class InvertedCondition extends QBaseCondition {

    Set<QCondition> conditions = new HashSet<>();

    @Override
    public boolean check(QPlayer player) {
        for (QCondition condition : conditions) {
            if (!condition.check(player)) {
                return success(player);
            }
        }
        return fail(player);
    }

    @Override
    public boolean check(QEvent event) {
        for (QCondition condition : conditions) {
            if (!condition.check(event)) {
                return success(event);
            }
        }
        return fail(event);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        conditions = ConditionManager.loadConditions("InvertedCondition " + section.getName(), section.getConfigurationSection("conditions"));
    }

}
