package de.erethon.questsxl.objective;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public class ObjectiveManager {

    public static Set<QObjective> loadObjectives(String id, ConfigurationSection section) {
        Set<QObjective> objectives = new HashSet<>();
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
            QObjective objective = null;
            switch (Objective.valueOf(type.toUpperCase())) {
                case CRAFT -> objective = new CraftObjective();
                case ENTER_REGION -> objective = new EnterRegionObjective();
                case ENTITY_INTERACT -> objective = new EntityInteractObjective();
                case ESCORT_NPC -> objective = new EscortNPCObjective();
                case EXPERIENCE -> objective = new ExperienceObjective();
                case IMPOSSIBLE -> objective = new ImpossibleObjective();
                case INSTANT -> objective = new InstantObjective();
                case ITEM_PICKUP -> objective = new ItemPickupObjective();
                case JOB_EXP -> objective = new JobExpObjective();
                case KILL_PLAYER -> objective = new KillPlayerObjective();
                case LEAVE_REGION -> objective = new LeaveRegionObjective();
                case LOCATION -> objective = new LocationObjective();
                case MOB -> objective = new MobObjective();
                case MYTHIC_MOB -> objective = new MythicMobObjective();
                case SERVER_COMMAND -> objective = new ServerCommandObjective();
                case USE_ITEM -> objective = new UseItemObjective();
                case WAIT -> objective = new WaitObjective();
            }
            try {
                if (shorthand) {
                    QLineConfig cfg = new QLineConfig(section.getString(key));
                    objective.load(cfg);
                } else {
                    objective.load(subsection);
                }
            } catch (Exception e) {
                QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Failed to load objective " + section.getName(), e.getMessage(), "Schaue im Stacktrace nach dem Fehler.").addStacktrace(e.getStackTrace()));
            }
            objectives.add(objective);
        }
        return objectives;
    }


    public static String[] split(String val) {
        return val.split(";");
    }

    public static boolean isValid(String key) {
        for (Objective objective : Objective.values()) {
            if (key.equalsIgnoreCase(objective.name())) {
                return true;
            }
        }
        return false;
    }

}
