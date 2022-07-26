package de.erethon.questsxl.objective;

import de.erethon.questsxl.quest.Completable;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface ObjectiveHolder {
    void addObjective(@NotNull ActiveObjective objective);
    boolean hasObjective(@NotNull QObjective objective);
    Set<ActiveObjective> getCurrentObjectives();
    void removeObjective(@NotNull ActiveObjective objective);
    void clearObjectives();
    void progress(@NotNull Completable completable);
    Location getLocation();
    String getName();
}
