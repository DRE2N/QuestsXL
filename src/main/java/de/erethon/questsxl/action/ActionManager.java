package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public class ActionManager {

    public static Set<QAction> loadActions(String id, ConfigurationSection section) {
        Set<QAction> actions = new HashSet<>();
        for (String key : section.getKeys(false)) {
            String type;
            ConfigurationSection subsection = null;
            boolean shorthand = false;
            if (isValid(key)) {
                type = key;
                shorthand = true;
            }
            else if (section.isConfigurationSection(key)) {
                subsection = section.getConfigurationSection(key);
                type = key;
            }
            else {
                subsection = section.getConfigurationSection(key);
                type = subsection.getString("type");
            }
            QAction action = Action.valueOf(type.toUpperCase()).newInstance();
            try {
                if (shorthand) {
                    QLineConfig cfg = new QLineConfig(section.getString(key));
                    action.load(cfg);
                } else {
                    action.load(subsection);
                }
            } catch (Exception e) {
                String loc = "";
                if (shorthand) {
                    loc = key;
                } else {
                    loc = subsection.getName();
                }
                QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Failed to load action " + section.getName(), e.getMessage(), "Pfad:\n" + section.getCurrentPath() + "." + loc).addStacktrace(e.getStackTrace()));
            }
            actions.add(action);
        }
        return actions;
    }

    public static String[] split(String val) {
        return val.split(";");
    }

    public static boolean isValid(String key) {
        for (Action action : Action.values()) {
            if (key.equalsIgnoreCase(action.name())) {
                return true;
            }
        }
        return false;
    }
}
