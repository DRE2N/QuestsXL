package de.erethon.questsxl.action;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public class ActionManager {

    public static Set<QAction> loadActions(ConfigurationSection section) {
        Set<QAction> actions = new HashSet<>();
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
            QAction action = null;
            switch (Action.valueOf(type.toUpperCase())) {
                case ANIMATION -> {
                    action = new QAnimation();
                }
                case COMMAND -> {
                    action = new QCommandAction();
                }
                case CUTSCENE -> {
                    action = new QCutscene();
                }
                case DELAY -> {
                    action = new DelayAction();
                }
                case GIVE_ITEM -> {
                    action = new GiveItemAction();
                }
                case HIDE_IBC -> {
                    action = new HideIBC();
                }
                case MESSAGE -> {
                    action = new SendMessage();
                }
                case MOB_FOLLOW_PLAYER -> {
                    action = new MobFollowPlayerAction();
                }
                case PERMISSION -> {
                    action = new QPermissionAction();
                }
                case SHOW_BEAM -> {
                    action = new QDisplayBeam();
                }
                case SHOW_IBC -> {
                    action = new ShowIBC();
                }
                case SPAWN_MOB -> {
                    action = new SpawnMobAction();
                }
                case STAGE -> {
                    action = new QStageAction();
                }
                case TELEPORT -> {
                    action = new QTeleportAction();
                }
                case TITLE -> {
                    action = new SendTitleAction();
                }
            }
            if (shorthand) {
                action.load(split(section.getString(key)));
            } else {
                action.load(subsection);
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
