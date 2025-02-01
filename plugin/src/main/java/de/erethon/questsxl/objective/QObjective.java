package de.erethon.questsxl.objective;

import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.condition.QCondition;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

import java.util.Set;

public interface QObjective extends Listener, QComponent {

    void check(ActiveObjective active, Event event);

    boolean isOptional();
    boolean isFailed();
    boolean isPersistent();
    Set<QAction> getSuccessActions();
    Set<QAction> getFailActions();
    Set<QAction> getConditionFailActions();
    Set<QCondition> getConditions();
    int getProgressGoal();

    void setGlobal(boolean global);
    boolean isGlobal();

    String getDisplayText();

    void onStart(ObjectiveHolder player);


}
