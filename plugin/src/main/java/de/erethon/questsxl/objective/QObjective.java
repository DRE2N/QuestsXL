package de.erethon.questsxl.objective;

import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.condition.QCondition;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.Set;

public interface QObjective<T extends Event> extends QComponent {

    /**
     * Returns the specific Event class that this objective needs to listen to.
     * This is called once when the objective is activated and is necessary
     * to work around Java's type erasure.
     *
     * @return The class of the event.
     */
    Class<T> getEventType();

    /**
     * The core logic of the objective. This is called by the ObjectiveEventManager
     * when the relevant event occurs. The event parameter is strongly-typed, so
     * no casting is needed.
     *
     * @param activeObjective The specific instance of the objective being checked (contains player, progress, etc.).
     * @param event The Bukkit event that was triggered.
     */
    void check(ActiveObjective activeObjective, T event);

    boolean isOptional();
    boolean isFailed();
    boolean isPersistent();
    boolean isHidden();
    Set<QAction> getCompleteActions();
    Set<QAction> getFailActions();
    Set<QAction> getConditionFailActions();
    Set<QCondition> getConditions();
    int getProgressGoal();

    void setGlobal(boolean global);
    boolean isGlobal();

    QTranslatable getDisplayText(Player player);

    void onStart(ObjectiveHolder player);


}
