package de.erethon.questsxl.objective;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class QBaseObjective implements QObjective {

    protected final QuestsXL plugin = QuestsXL.getInstance();
    private String displayText;
    private final Set<QAction> successActions = new HashSet<>();
    private final Set<QAction> progressActions = new HashSet<>();
    private final Set<QCondition> conditions = new HashSet<>();
    private final Set<QAction> conditionFailActions = new HashSet<>();
    private final Set<QAction> failActions = new HashSet<>();
    private boolean failed = false;
    private boolean optional = false;
    private boolean persistent = false;
    private boolean isGlobal = false;

    protected int progressGoal = 0;

    /**
     * Checks for the conditions of this objective. If all conditions are met, the objective can be completed.
     * For QEvents, conditions are checked per player, but the {@link ObjectiveHolder} is a QEvent.
     * @param player the player to check the conditions for
     * @return true if the conditions are met
     */
    public boolean conditions(Player player) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        QPlayer qPlayer = plugin.getPlayerCache().getByPlayer(player);
        for (QCondition condition : conditions) {
            if (!condition.check(qPlayer)) {
                condFail(qPlayer);
                return false;
            }
        }
        return true;
    }

    protected void checkCompletion(ActiveObjective active, QObjective objective) {
        if (active.getProgress() >= progressGoal) {
            complete(active.getHolder(), objective);
        }
    }

    /**
     * Completes an objective. Completing removes it from the current stage (if not persistent) and progresses
     * to the next stage (if available).
     * Objective completion will also execute success actions, if any.
     * @param holder the holder that completed the objective
     * @param obj the objective that was completed.
     */
    protected void complete(ObjectiveHolder holder, QObjective obj) {
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
        if (holder instanceof QPlayer qPlayer) {
            for (QAction action : successActions) {
                action.play(qPlayer);
            }
        } else if (holder instanceof QEvent event) {
            for (QAction action : successActions) {
                action.play(event);
            }
        }
    }

    public void progress(ObjectiveHolder holder) {
        for (QAction action : progressActions) {
            if (holder instanceof QPlayer qPlayer) {
                action.play((QPlayer) holder);
            } else if (holder instanceof QEvent event) {
                action.play((QEvent) holder);
            }
        }
    }

    private void condFail(QPlayer player) {
        for (QAction action : conditionFailActions) {
            action.play(player);
        }
    }

    /**
     * @param holder the ObjectiveHolder to run the actions for
     * @param obj the QObjective that failed
     */
    public void fail(ObjectiveHolder holder, QObjective obj) {
        for (ActiveObjective objective : holder.getCurrentObjectives()) {
            if (objective.getObjective().equals(obj) && objective.getHolder().equals(holder)) {
                if (!persistent) {
                    holder.getCurrentObjectives().remove(objective);
                }
            }
        }
        for (QAction action : failActions) {
            if (holder instanceof QPlayer qPlayer) {
                action.play(qPlayer);
            } else if (holder instanceof QEvent event) {
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
    public Set<QAction> getSuccessActions() {
        return successActions;
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
    public void load(QLineConfig section) {
        if (section.contains("goal")) {
            progressGoal = section.getInt("goal");
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        if (section.contains("display")) {
            displayText = section.getString("display");
        }
        if (section.contains("conditions")) {
            conditions.addAll((Collection<? extends QCondition>) QConfigLoader.load("conditions", section, QRegistries.CONDITIONS));
        }
        if (section.contains("onProgress")) {
            progressActions.addAll((Collection<? extends QAction>) QConfigLoader.load("onProgress", section, QRegistries.ACTIONS));
        }
        if (section.contains("onConditionFail")) {
            conditionFailActions.addAll((Collection<? extends QAction>) QConfigLoader.load("onConditionFail", section, QRegistries.ACTIONS));
        }
        if (section.contains("onComplete")) {
            successActions.addAll((Collection<? extends QAction>) QConfigLoader.load("onComplete", section, QRegistries.ACTIONS));
        }
        if (section.contains("onFail")) {
            failActions.addAll((Collection<? extends QAction>) QConfigLoader.load("onFail", section, QRegistries.ACTIONS));
        }
        if (section.contains("optional")) {
            optional = section.getBoolean("optional");
        }
        if (section.contains("persistent")) {
            persistent = section.getBoolean("persistent");
        }
        if (section.contains("global")) {
            isGlobal = section.getBoolean("global");
        }
        if (section.contains("goal")) {
            progressGoal = section.getInt("goal");
        }
    }

    @Override
    public void onStart(ObjectiveHolder player) {

    }
}
