package de.erethon.questsxl.objective;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.HashSet;
import java.util.Set;

enum ActionScope {
    PLAYER,
    EVENT
}

public abstract class QBaseObjective<T extends Event> implements QObjective<T> {

    protected final QuestsXL plugin = QuestsXL.get();
    private QTranslatable displayText;
    private final Set<QAction> completeActions = new HashSet<>();
    private final Set<QAction> progressActions = new HashSet<>();
    private final Set<QCondition> conditions = new HashSet<>();
    private final Set<QAction> conditionFailActions = new HashSet<>();
    private final Set<QAction> failActions = new HashSet<>();
    private ActionScope completeScope = ActionScope.PLAYER;
    private ActionScope progressScope = ActionScope.PLAYER;
    private ActionScope conditionFailScope = ActionScope.PLAYER;
    private ActionScope failScope = ActionScope.PLAYER;
    private final boolean failed = false;
    private boolean optional = false;
    private boolean persistent = false;
    private boolean isGlobal = false;
    private boolean isHidden = false;
    protected boolean shouldCancelEvent = false;

    private QComponent parent;

    protected int progressGoal = 1;

    /**
     * Checks for the conditions of this objective. If all conditions are met, the objective can be completed.
     *
     * @param holder the ObjectiveHolder to check the conditions for
     * @return true if the conditions are met
     */
    public boolean conditions(ObjectiveHolder holder, ObjectiveHolder instigator) {
        if (conditions.isEmpty()) {
            return true;
        }
        for (QCondition condition : conditions) {
            if (holder instanceof QPlayer qPlayer) {
                try {
                    if (!condition.check(qPlayer)) {
                        condFail(qPlayer, instigator);
                        return false;
                    }
                } catch (Exception e) {
                    FriendlyError error = new FriendlyError("Condition check failed", "Condition check failed for " + qPlayer.getName(), e.getMessage(), "Condition: " + condition.getClass().getName());
                    error.addPlayer(qPlayer);
                    error.addStacktrace(e.getStackTrace());
                    return false;
                }
            } else if (holder instanceof QEvent event) {
                if (!condition.check(event)) {
                    condFail(event, instigator);
                    return false;
                }
            }
        }
        return true;
    }

    public boolean conditions(Player player) {
        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        return conditions(qPlayer, qPlayer);
    }

    /**
     * Checks if the objective is completed. If the objective is completed, the objective is removed from the current
     * stage and the next stage is progressed to.
     *
     * @param active the ActiveObjective to check
     * @param objective the QObjective to check
     * @param instigator the ObjectiveHolder that instigated the completion, e.g., the player that completed the objective
     */
    protected void checkCompletion(ActiveObjective active, QObjective<T> objective, ObjectiveHolder instigator) {
        active.addProgress(1);
        progress(active.getHolder(), instigator);
        QuestsXL.log("Progress: " + active.getProgress() + " Goal: " + progressGoal + " Scope: " + progressScope);

        // Save progress to database
        active.saveToDatabase();

        if (active.getProgress() >= progressGoal) {
            complete(active.getHolder(), objective, instigator);
        }
    }

    /**
     * Checks if the objective is completed, using the holder (QEvent) as the instigator.
     * This is useful when non-player entities trigger objective progression (e.g., wolves killing villagers).
     *
     * @param active the ActiveObjective to check
     * @param objective the QObjective to check
     */
    protected void checkCompletion(ActiveObjective active, QObjective<T> objective) {
        checkCompletion(active, objective, active.getHolder());
    }

    /**
     * Completes an objective. Completing removes it from the current stage (if not persistent) and progresses
     * to the next stage (if available).
     * Objective completion will also execute success actions, if any.
     * @param holder the holder that completed the objective
     * @param obj the objective that was completed.
     */
    protected void complete(ObjectiveHolder holder, QObjective<T> obj, ObjectiveHolder instigator) {
        QuestsXL.log("Checking for completion for " + holder.getName());
        Set<ActiveObjective> activeObjectives = holder.getCurrentObjectives();
        Set<ActiveObjective> toRemove = new HashSet<>();
        // Create a copy to avoid ConcurrentModificationException when checkCompleted modifies the set
        for (ActiveObjective activeObjective : new HashSet<>(activeObjectives)) {
            QuestsXL.log("Active: Objective: " + activeObjective.getObjective().getClass().getName() + " Holder: " + activeObjective.getHolder().getName() + " | Objective: " + obj.getClass().getName() + " Holder: " + holder.getName());
            if (activeObjective.getObjective() == obj && activeObjective.getHolder() == holder) {
                activeObjective.setCompleted(true);
                // Save completion status to database
                activeObjective.saveToDatabase();
                QuestsXL.log("Completed " + obj.getClass().getName());
                if (activeObjective.getStage() != null) {
                    activeObjective.getStage().checkCompleted(holder);
                }
                if (!persistent) {
                    toRemove.add(activeObjective);
                }
            }
        }
        for (ActiveObjective activeObjective : toRemove) {
            activeObjectives.remove(activeObjective);
            plugin.getObjectiveEventManager().unregister(activeObjective);
            // Remove from database when objective is removed
            activeObjective.removeFromDatabase();
        }
        if (completeScope == ActionScope.PLAYER) {
            runActions(completeActions, instigator);
        }
        else if (completeScope == ActionScope.EVENT) {
            runActions(completeActions, holder);
        }
    }

    public void progress(ObjectiveHolder holder, ObjectiveHolder instigator) {
        if (progressScope == ActionScope.PLAYER) {
            runActions(progressActions, instigator);
        }
        else if (progressScope == ActionScope.EVENT) {
            runActions(progressActions, holder);
        }
    }


    private void condFail(ObjectiveHolder holder, ObjectiveHolder instigator) {
        if (conditionFailScope == ActionScope.PLAYER) {
            runActions(conditionFailActions, instigator);
        }
        else if (conditionFailScope == ActionScope.EVENT) {
            runActions(conditionFailActions, holder);
        }
    }

    /**
     * @param holder the ObjectiveHolder to run the actions for
     * @param obj the QObjective that failed
     */
    public void fail(ObjectiveHolder holder, QObjective<T> obj, ObjectiveHolder instigator) {
        Set<ActiveObjective> activeObjectives = holder.getCurrentObjectives();
        Set<ActiveObjective> toRemove = new HashSet<>();
        for (ActiveObjective objective : activeObjectives) {
            if (objective.getObjective().equals(obj) && objective.getHolder().equals(holder)) {
                if (!persistent) {
                    toRemove.add(objective);
                }
            }
        }
        for (ActiveObjective objective : toRemove) {
            activeObjectives.remove(objective);
        }
        if (failScope == ActionScope.PLAYER) {
            runActions(failActions, instigator);
        }
        else if (failScope == ActionScope.EVENT) {
            runActions(failActions, holder);
        }
    }

    private void runActions(Set<QAction> actions, ObjectiveHolder instigator) {
        for (QAction action : actions) {
            if (instigator instanceof QPlayer player) {
                try {
                    action.play(player);
                } catch (Exception e) {
                    FriendlyError error = new FriendlyError(instigator.getName(), "Failed to run action for " + player.getName(), e.getMessage(), "Action: " + action.getClass().getName());
                    error.addPlayer(player);
                    error.addStacktrace(e.getStackTrace());
                    QuestsXL.get().getErrors().add(error);
                }
            }
            else if (instigator instanceof QEvent event) {
                try {
                    action.play(event);
                } catch (Exception e) {
                    FriendlyError error = new FriendlyError(instigator.getName(), "Failed to run action for " + event.getName(), e.getMessage(), "Action: " + action.getClass().getName());
                    error.addStacktrace(e.getStackTrace());
                    QuestsXL.get().getErrors().add(error);
                }
            }
        }
    }

    /**
     * @return if the objective has failed
     */
    @Override
    public boolean isFailed() {
        return failed;
    }

    /**
     * @return if the objective is optional. Optional objectives are not needed for stage completion.
     */
    @Override
    public boolean isOptional() {
        return optional;
    }

    /**
     * @return if the objective is persistent. Persistent objectives are not removed after completion.
     */
    @Override
    public boolean isPersistent() {
        return persistent;
    }

    /**
     * Sets whether the objective is persistent. Persistent objectives are not removed after completion.
     * @param persistent true if the objective should be persistent
     */
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    /**
     * @return a set of actions that are run when the objective is completed.
     */
    @Override
    public Set<QAction> getCompleteActions() {
        return completeActions;
    }

    /**
     * @return a set of actions that are run when the objective has failed.
     */
    @Override
    public Set<QAction> getFailActions() {
        return failActions;
    }

    /**
     * @return the text that is displayed to the player.
     */
    @Override
    public QTranslatable getDisplayText(Player player) {
        if (displayText != null) {
            return displayText;
        }
        return getDefaultDisplayText(player);
    }

    /**
     * Generates the default display text for this objective type.
     * This should be overridden by specific objective implementations to provide
     * meaningful default text when no custom display text is configured.
     *
     * @return the default translatable display text for this objective
     */
    protected abstract QTranslatable getDefaultDisplayText(Player player);

    /**
     * @return a set of conditions that are checked. Conditions are checked per player.
     */
    public Set<QCondition> getConditions() {
        return conditions;
    }

    /**
     * @return a set of actions that are run when the conditions are not met.
     */
    @Override
    public Set<QAction> getConditionFailActions() {
        return conditionFailActions;
    }

    /**
     * @return if the objective is global, e.g. does not require a Quest or Event.
     */
    @Override
    public boolean isGlobal() {
        return isGlobal;
    }

    @Override
    public void setGlobal(boolean global) {
        isGlobal = global;
    }

    /**
     * @return if the objective is hidden from the player.
     */
    @Override
    public boolean isHidden() {
        return isHidden;
    }

    /**
     * @return the progress goal of the objective.
     */
    @Override
    public int getProgressGoal() {
        return progressGoal;
    }

    @Override
    public QComponent getParent() {
        return parent;
    }

    @Override
    public String id() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void setParent(QComponent parent) {
        this.parent = parent;
    }

    @Override
    public void load(QConfig cfg) {
        if (cfg.contains("display")) {
            displayText = QTranslatable.fromString(cfg.getString("display"));
        }
        if (cfg.contains("hidden")) {
            isHidden = cfg.getBoolean("hidden", false);
        }
        if (cfg.contains("cancel")) {
            shouldCancelEvent = cfg.getBoolean("cancel");
        }
        if (cfg.contains("conditions")) {
            conditions.addAll(cfg.getConditions(this, "conditions"));
            QuestsXL.log("Loaded conditions for " + this.getClass().getSimpleName() + ": " + conditions.size());
        }
        if (cfg.contains("scopeSuccess")) {
            completeScope = ActionScope.valueOf(cfg.getString("scopeSuccess").toUpperCase());
        } else {
            if (findTopParent() instanceof QEvent) {
                completeScope = ActionScope.EVENT;
            } else {
                completeScope = ActionScope.PLAYER;
            }
        }
        if (cfg.contains("scopeProgress")) {
            progressScope = ActionScope.valueOf(cfg.getString("scopeProgress").toUpperCase());
        } else {
            QComponent topParent = findTopParent();
            if (topParent instanceof QEvent) {
                progressScope = ActionScope.EVENT;
            } else {
                progressScope = ActionScope.PLAYER;
            }
        }
        if (cfg.contains("onProgress")) {
            progressActions.addAll(cfg.getActions(this, "onProgress"));
            QuestsXL.log("Loaded progress actions for " + this.getClass().getSimpleName() + ": " + progressActions.size());
        }
        if (cfg.contains("scopeConditionFail")) {
            conditionFailScope = ActionScope.valueOf(cfg.getString("scopeConditionFail").toUpperCase());
        } else {
            if (findTopParent() instanceof QEvent) {
                conditionFailScope = ActionScope.EVENT;
            } else {
                conditionFailScope = ActionScope.PLAYER;
            }
        }
        if (cfg.contains("onConditionFail")) {
            conditionFailActions.addAll(cfg.getActions(this, "onConditionFail"));
        }
        if (cfg.contains("scopeFail")) {
            failScope = ActionScope.valueOf(cfg.getString("scopeFail").toUpperCase());
        } else {
            if (findTopParent() instanceof QEvent) {
                failScope = ActionScope.EVENT;
            } else {
                failScope = ActionScope.PLAYER;
            }
        }
        if (cfg.contains("onFail")) {
            failActions.addAll(cfg.getActions(this, "onFail"));
        }
        if (cfg.contains("scopeComplete")) {
            completeScope = ActionScope.valueOf(cfg.getString("scopeComplete").toUpperCase());
        } else {
            if (findTopParent() instanceof QEvent) {
                completeScope = ActionScope.EVENT;
            } else {
                completeScope = ActionScope.PLAYER;
            }
        }
        if (cfg.contains("onComplete")) {
            completeActions.addAll(cfg.getActions(this, "onComplete"));
        }
        if (cfg.contains("optional")) {
            optional = cfg.getBoolean("optional");
        }
        if (cfg.contains("persistent")) {
            persistent = cfg.getBoolean("persistent");
        }
        if (cfg.contains("global")) {
            isGlobal = cfg.getBoolean("global");
        }
        if (cfg.contains("goal")) {
            progressGoal = cfg.getInt("goal");
        }
    }

    @Override
    public void onStart(ObjectiveHolder player) {

    }
}
