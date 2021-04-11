package de.erethon.questsxl.objectives;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public class ObjectiveManager {

    public static Set<QObjective> loadObjectives(ConfigurationSection section) {
        Set<QObjective> objectives = new HashSet<>();
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
            QObjective objective = null;
            switch (Objective.valueOf(type.toUpperCase())) {
                case CRAFT -> {
                    objective = new CraftObjective();
                }
                case ENTITY_INTERACT -> {
                    objective = new EntityInteractObjective();
                }
                case ESCORT_NPC -> {
                    objective = new EscortNPCObjective();
                }
                case EXPERIENCE -> {
                     objective = new ExperienceObjective();
                }
                case INSTANT -> {
                    objective = new InstantObjective();
                }
                case ITEM_PICKUP -> {
                    objective = new ItemPickupObjective();
                }
                case JOB_EXP -> {
                    objective = new JobExpObjective();
                }
                case KILL_PLAYER -> {
                    objective = new KillPlayerObjective();
                }
                case LOCATION -> {
                    objective = new LocationObjective();
                }
                case MOB -> {
                    objective = new MobObjective();
                }
                case MYTHIC_MOB -> {
                    objective = new MythicMobObjective();
                }
                case SERVER_COMMAND -> {
                    objective = new ServerCommandObjective();
                }
                case USE_ITEM -> {
                    objective = new UseItemObjective();
                }
                case WAIT -> {
                    objective = new WaitObjective();
                }
            }
            if (shorthand) {
                objective.load(split(section.getString(key)));
            } else {
                objective.load(subsection);
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
