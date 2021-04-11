package de.erethon.questsxl.listener;

import de.erethon.questsxl.objectives.ActiveObjective;
import de.erethon.questsxl.objectives.QObjective;

import java.util.HashSet;
import java.util.Set;

public class ListenerManager {

    Set<ActiveObjective> objectives = new HashSet<>();

    public void addActive(ActiveObjective objective) {
        objectives.add(objective);
    }

    public void removeActive(ActiveObjective objective) {
        objectives.remove(objective);
    }
}
