package de.erethon.questsxl.common;

import de.erethon.questsxl.objective.QObjective;
import org.bukkit.Location;

import java.util.Set;

/**
 * A quester is something that can be assigned objectives.
 * For example, Players and Events are Questers.
 */
public interface Quester {

    default Set<QObjective> objectives() {
        return Set.of();
    }

    String getName();

    Location getLocation();
}
