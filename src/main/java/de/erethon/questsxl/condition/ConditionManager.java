package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public class ConditionManager {

    public static Set<QCondition> loadConditions(String id, ConfigurationSection section) {
        Set<QCondition> conditions = new HashSet<>();
        for (String key : section.getKeys(false)) {
            String type;
            ConfigurationSection subsection = null;
            boolean shorthand = false;
            if (isValid(key)) {
                type = key;
                shorthand = true;
            }
            else if (section.isConfigurationSection(key) && isValid(key)) {
                subsection = section.getConfigurationSection(key);
                type = key;
            }
            else {
                subsection = section.getConfigurationSection(key);
                type = subsection.getString("type");
            }
            QCondition condition = null;
            switch (Condition.valueOf(type.toUpperCase())) {
                case EVENT_STATE -> condition = new EventStateCondition();
                case GLOBAL_SCORE -> condition = new GlobalScoreCondition();
                case GROUP_SIZE -> condition = new GroupSizeCondition();
                case INVENTORY -> condition = new InventoryCondition();
                case INVERTED -> condition = new InvertedCondition();
                case JOB_LEVEL -> condition = new JobLevelCondition();
                case LEVEL -> condition = new LevelCondition();
                case LOCATION -> condition = new LocationCondition();
                case LOOKING_AT -> condition = new LookingAtCondition();
                case PERMISSION -> condition = new PermissionCondition();
                case PLAYER_SCORE -> condition = new PlayerScoreCondition();
                case ACTIVE_QUEST -> condition = new ActiveQuestCondition();
                case COMPLETED_QUEST -> condition = new CompletedQuestCondition();
                case REGION -> condition = new RegionCondition();
                case TIME -> condition = new TimeCondition();
            }
            try {
                if (shorthand) {
                    QLineConfig cfg = new QLineConfig(section.getString(key));
                    condition.load(cfg);
                } else {
                    condition.load(subsection);
                }
            } catch (Exception e) {
                QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Failed to load condition " + section.getName(), e.getMessage(), "Schaue im Stacktrace nach dem Fehler.").addStacktrace(e.getStackTrace()));
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
