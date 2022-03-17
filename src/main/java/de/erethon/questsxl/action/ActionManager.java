package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
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
            } else {
                subsection = section.getConfigurationSection(key);
                type = subsection.getString("type");
            }
            QAction action = null;
            switch (Action.valueOf(type.toUpperCase())) {
                case ANIMATION -> action = new PlayAnimationAction();
                case COMMAND -> action = new RunCommandAction();
                case CUTSCENE -> action = new PlayCutsceneAction();
                case DELAY -> action = new DelayAction();
                case GIVE_ITEM -> action = new GiveItemAction();
                case HIDE_IBC -> action = new HideIBC();
                case JOB_EXP -> action = new JobExpAction();
                case MESSAGE -> action = new SendMessage();
                case MOB_FOLLOW_PLAYER -> action = new MobFollowPlayerAction();
                case PASTE_SCHEMATIC -> action = new PasteSchematicAction();
                case PERMISSION -> action = new PermissionAction();
                case REPEAT -> action = new RepeatAction();
                case RESET_IBC -> action = new ResetIBC();
                case SHOW_BEAM -> action = new DisplayLocationMarkerAction();
                case SHOW_IBC -> action = new ShowIBC();
                case SPAWN_MOB -> action = new SpawnMobAction();
                case STAGE -> action = new StageAction();
                case START_QUEST -> action = new QuestAction();
                case TELEPORT -> action = new TeleportPlayerAction();
                case TITLE -> action = new SendTitleAction();
            }
            try {
                if (shorthand) {
                    action.load(split(section.getString(key)));
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
