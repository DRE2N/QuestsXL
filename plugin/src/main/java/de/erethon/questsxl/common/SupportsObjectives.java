package de.erethon.questsxl.common;

import de.erethon.questsxl.objective.QBaseObjective;
import de.erethon.questsxl.objective.QObjective;

public interface SupportsObjectives {

    void addObjective(QObjective<?> objective);

    void removeObjective(QObjective<?> objective);
}
