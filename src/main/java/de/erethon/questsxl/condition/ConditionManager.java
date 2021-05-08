package de.erethon.questsxl.condition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public class ConditionManager {

    public static Set<QCondition> loadConditions(ConfigurationSection section) {
        Set<QCondition> conditions = new HashSet<>();
        for (String key : section.getKeys(false)) {
            String type;
            ConfigurationSection subsection = null;
            boolean shorthand = false;
            if (isValid(key)) {
                type = key;
                shorthand = true;
            } else {
                subsection = section.getConfigurationSection(key);
                type = subsection.getString("type");
            }
            QCondition condition = null;
            switch (Condition.valueOf(type.toUpperCase())) {
                case GLOBAL_VARIABLE -> {
                    condition = new GlobalVariableCondition();
                }
                case GROUP_SIZE -> {
                    condition = new GroupSizeCondition();
                }
                case INVENTORY -> {
                    condition = new InventoryCondition();
                }
                case INVERTED -> {
                    condition = new InvertedCondition();
                }
                case LEVEL -> {
                    condition = new LevelCondition();
                }
                case LOCATION -> {
                    condition = new LocationCondition();
                }
                case LOOKING_AT -> {
                    condition = new LookingAtCondition();
                }
                case PERMISSION -> {
                    condition = new PermissionCondition();
                }
                case PLAYER_VARIABLE -> {
                    condition = new PlayerVariableCondition();
                }
                case ACTIVE_QUEST -> {
                    condition = new ActiveQuestCondition();
                }
                case COMPLETED_QUEST -> {
                    condition = new CompletedQuestCondition();
                }
                case TIME -> {
                    condition = new TimeCondition();
                }
            }
            if (shorthand) {
                condition.load(split(section.getString(key)));
            } else {
                condition.load(subsection);
            }
            conditions.add(condition);
        }
        return conditions;
    }

    public static String[] split(String val) {
        return val.split(";");
    }

    public static boolean isValid(String key) {
        for (Condition condition : Condition.values()) {
            if (key.equalsIgnoreCase(condition.name())) {
                return true;
            }
        }
        return false;
    }
}
