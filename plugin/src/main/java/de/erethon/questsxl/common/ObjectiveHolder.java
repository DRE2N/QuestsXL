package de.erethon.questsxl.common;

import de.erethon.questsxl.component.objective.ActiveObjective;
import de.erethon.questsxl.component.objective.QObjective;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Represents an object that can hold objectives.
 */
public interface ObjectiveHolder {
    void addObjective(@NotNull ActiveObjective objective);
    boolean hasObjective(@NotNull QObjective objective);
    Set<ActiveObjective> getCurrentObjectives();
    void removeObjective(@NotNull ActiveObjective objective);
    void clearObjectives();
    void progress(@NotNull Completable completable);
    Location getLocation();
    String getName();
    String getUniqueId();


    default ActiveObjective findObjective(QObjective objective) {
        for (ActiveObjective activeObjective : getCurrentObjectives()) {
            if (activeObjective.getObjective().equals(objective)) {
                return activeObjective;
            }
        }
        return null;
    }
}
