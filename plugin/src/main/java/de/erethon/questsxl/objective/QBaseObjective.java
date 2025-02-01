package de.erethon.questsxl.objective;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

enum ActionScope {
    PLAYER,
    EVENT
}

public abstract class QBaseObjective implements QObjective {

    protected final QuestsXL plugin = QuestsXL.getInstance();
    private String displayText;
    private final Set<QAction> completeActions = new HashSet<>();
    private final Set<QAction> progressActions = new HashSet<>();
    private final Set<QCondition> conditions = new HashSet<>();
    private final Set<QAction> conditionFailActions = new HashSet<>();
    private final Set<QAction> failActions = new HashSet<>();
    private ActionScope completeScope = ActionScope.PLAYER;
    private ActionScope progressScope = ActionScope.PLAYER;
    private ActionScope conditionFailScope = ActionScope.PLAYER;
    private ActionScope failScope = ActionScope.PLAYER;
    private boolean failed = false;
    private boolean optional = false;
    private boolean persistent = false;
    private boolean isGlobal = false;
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
                if (!condition.check(qPlayer)) {
                    condFail(qPlayer, instigator);
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
        QPlayer qPlayer = plugin.getPlayerCache().getByPlayer(player);
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
    protected void checkCompletion(ActiveObjective active, QObjective objective, ObjectiveHolder instigator) {
        active.addProgress(1);
        progress(active.getHolder(), instigator);
        MessageUtil.log("Progress: " + active.getProgress() + " Goal: " + progressGoal);
        if (active.getProgress() >= progressGoal) {
            complete(active.getHolder(), objective, instigator);
        }
    }

    /**
     * Completes an objective. Completing removes it from the current stage (if not persistent) and progresses
     * to the next stage (if available).
     * Objective completion will also execute success actions, if any.
     * @param holder the holder that completed the objective
     * @param obj the objective that was completed.
     */
    protected void complete(ObjectiveHolder holder, QObjective obj, ObjectiveHolder instigator) {
        MessageUtil.log("Checking for completion for " + holder.getName());
        Iterator<ActiveObjective> iterator = holder.getCurrentObjectives().iterator();
        while (iterator.hasNext()) {
            ActiveObjective activeObjective = iterator.next();
            MessageUtil.log("Active: Objective: " + activeObjective.getObjective().getClass().getName() + " Holder: " + activeObjective.getHolder().getName() + " | Objective: " + obj.getClass().getName() + " Holder: " + holder.getName());
            if (activeObjective.getObjective() == obj && activeObjective.getHolder() == holder) {
                activeObjective.setCompleted(true);
                MessageUtil.log("Completed " + obj.getClass().getName());
                if (activeObjective.getStage() != null) {
                    activeObjective.getStage().checkCompleted(holder);
                }
                if (!persistent) {
                    iterator.remove();
                }
            }
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
    public void fail(ObjectiveHolder holder, QObjective obj, ObjectiveHolder instigator) {
        for (ActiveObjective objective : holder.getCurrentObjectives()) {
            if (objective.getObjective().equals(obj) && objective.getHolder().equals(holder)) {
                if (!persistent) {
                    holder.getCurrentObjectives().remove(objective);
                }
            }
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
                action.play(player);
            }
            else if (instigator instanceof QEvent event) {
                action.play(event);
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
    public String getDisplayText() {
        return displayText;
    }

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
    public void setParent(QComponent parent) {
        this.parent = parent;
    }

    @Override
    public void load(QConfig cfg) {
        if (cfg.contains("display")) {
            displayText = cfg.getString("display");
        }
        if (cfg.contains("cancel")) {
            shouldCancelEvent = cfg.getBoolean("cancel");
        }
        if (cfg.contains("scope_success")) {
            completeScope = ActionScope.valueOf(cfg.getString("scope").toUpperCase());
        }
        if (cfg.contains("onSuccess")) {
            completeActions.addAll(cfg.getActions(this, "onSuccess"));
        }
        if (cfg.contains("scope_progress")) {
            progressScope = ActionScope.valueOf(cfg.getString("scope_progress").toUpperCase());
        }
        if (cfg.contains("onProgress")) {
            progressActions.addAll(cfg.getActions(this, "onProgress"));
        }
        if (cfg.contains("scope_conditionFail")) {
            conditionFailScope = ActionScope.valueOf(cfg.getString("scope_conditionFail").toUpperCase());
        }
        if (cfg.contains("onConditionFail")) {
            conditionFailActions.addAll(cfg.getActions(this, "onConditionFail"));
        }
        if (cfg.contains("scope_fail")) {
            failScope = ActionScope.valueOf(cfg.getString("scope_fail").toUpperCase());
        }
        if (cfg.contains("onFail")) {
            failActions.addAll(cfg.getActions(this, "onFail"));
        }
        if (cfg.contains("scope_Complete")) {
            completeScope = ActionScope.valueOf(cfg.getString("scope_fail").toUpperCase());
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
