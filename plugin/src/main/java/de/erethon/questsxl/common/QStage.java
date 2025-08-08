package de.erethon.questsxl.common;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class QStage implements QComponent {

    private final QuestsXL plugin = QuestsXL.get();

    private final Completable owner;
    private final Set<QObjective> goals = new HashSet<>();
    private final Set<QCondition> conditions = new HashSet<>();
    private final Set<QAction> startActions = new HashSet<>();
    private final Set<QAction> completeActions = new HashSet<>();
    private final int id;
    private String startMessage = "";
    private String description = "";
    private String completeMessage = "";

    private QComponent parent;

    /**
     * A stage is a collection of objectives that are completed in order to progress to the next stage. Stages can
     * belong to both a {@link QEvent} and a {@link QQuest}.
     * @param completable the completable that owns this stage.
     * @param id the id of this stage, must be unique within the completable.
     */
    public QStage(Completable completable,  int id) {
        this.owner = completable;
        this.id = id;
    }

    /** Start the stage. This will execute all start actions, and then adds the objectives to the holder.
     * @param holder the holder to start the stage.
     */
    public void start(ObjectiveHolder holder) {
        QuestsXL.log("Starting stage " + id);
        for (QObjective objective : goals) {
            QuestsXL.log("Added objective " + objective.getClass().getName());
            ActiveObjective activeObjective = new ActiveObjective(holder, getQuest(), this, objective);
            holder.addObjective(activeObjective);
            plugin.getObjectiveEventManager().register(activeObjective);
        }
        try {
            if (holder instanceof QPlayer player) {
                for (QAction action : startActions) {
                    action.play(player);
                }
            }
            if (holder instanceof QEvent event) {
                for (QAction action : startActions) {
                    action.play(event);
                }
            }
        }
        catch (Exception e) {
            FriendlyError error = new FriendlyError(holder.getName() + ".stages." + id, "Failed to start stage " + id, e.getMessage(), "" + id).addStacktrace(e.getStackTrace());
            QuestsXL.get().addRuntimeError(error);
            QuestsXL.log("Failed to start stage " + id + " for " + holder.getName());
            e.printStackTrace();
        }
    }

    /** Checks for completions. This is called every time an objective is completed.
     * @param holder the holder that completed the objective.
     */
    public void checkCompleted(ObjectiveHolder holder) {
       if (isCompleted(holder)) {
           QuestsXL.log("Stage is completed!");
           holder.progress(owner);
           return;
       }
       QuestsXL.log("Stage not completed");
    }

    /**
     * @deprecated Currently unused.
     * @return true if the loadable can start this stage, false otherwise.
     */
    @Deprecated
    public boolean canStart(ObjectiveHolder holder) {
        Set<QCondition> failed = new HashSet<>();
        boolean canStart = true;
        if (holder instanceof QPlayer qPlayer) {
            for (QCondition condition : conditions) {
                try {
                    if (!condition.check(qPlayer)) {
                        canStart = false;
                        failed.add(condition);
                    }
                } catch (Exception e) {
                    FriendlyError error = new FriendlyError(holder.getName() + ".stages." + id, "Failed to check condition " + condition.getClass().getName(), e.getMessage(), "" + id).addStacktrace(e.getStackTrace());
                    QuestsXL.get().addRuntimeError(error);
                    QuestsXL.log("Failed to check condition " + condition.getClass().getName() + " for " + holder.getName());
                }
            }
            return canStart;
        }
        if (holder instanceof QEvent qEvent) {
            for (QCondition condition : conditions) {
                try {
                    if (!condition.check(qEvent)) {
                        canStart = false;
                        failed.add(condition);
                    }
                } catch (Exception e) {
                    FriendlyError error = new FriendlyError(qEvent.getName() + ".stages." + id, "Failed to check condition " + condition.getClass().getName(), e.getMessage(), "" + id).addStacktrace(e.getStackTrace());
                    QuestsXL.get().addRuntimeError(error);
                    QuestsXL.log("Failed to check condition " + condition.getClass().getName() + " for " + qEvent.getName());
                }
            }
            return canStart;
        }
        return false;
    }

    /** Checks if all objectives are completed for this stage.
     * @param holder the holder to check.
     * @return true if the holder has completed this stage, false otherwise.
     */
    public boolean isCompleted(ObjectiveHolder holder) {
        for (ActiveObjective activeObjective : holder.getCurrentObjectives()) {
            if (activeObjective.getStage() == null) {
                continue;
            }
            if (goals.contains(activeObjective.getObjective())) {
                if (!activeObjective.isCompleted() && !activeObjective.getObjective().isOptional()) {
                    return false;
                }
            }
        }
        for (QAction qAction : completeActions) {
            if (holder instanceof QPlayer player) {
                qAction.play(player);
            }
            if (holder instanceof QEvent event) {
                qAction.play(event);
            }
        }
        return true;
    }


    public boolean hasObjective(QObjective obj) {
        return goals.contains(obj);
    }

    /**
     * @return a unique id for this stage.
     */
    public int getId() {
        return id;
    }

    public Completable getOwner() {
        return owner;
    }

    public Set<QObjective> getGoals() {
        return goals;
    }

    public Set<QCondition> getConditions() {
        return conditions;
    }

    public String getStartMessage() {
        return startMessage;
    }

    public String getCompleteMessage() {
        return completeMessage;
    }

    public QQuest getQuest() {
        return owner instanceof QQuest ? (QQuest) owner : null;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public QComponent getParent() {
        return parent;
    }

    @Override
    public void setParent(QComponent parent) {
        this.parent = parent;
    }

    public void load(QComponent component, ConfigurationSection section) {
        setParent(component);
        startMessage = section.getString("startMessage", "");
        completeMessage = section.getString("completeMessage", "");
        description = section.getString("description", "");
        if (section.contains("conditions")) {
            conditions.addAll((Collection<? extends QCondition>) QConfigLoader.load(this, "conditions", section, QRegistries.CONDITIONS));
            for (QCondition condition : conditions) {
                condition.setParent(this);
            }
        }
        if (section.contains("objectives")) {
            goals.addAll((Collection<? extends QObjective>) QConfigLoader.load(this, "objectives", section, QRegistries.OBJECTIVES));
            for (QObjective objective : goals) {
                objective.setParent(this);
            }
        }
        if (section.contains("onStart")) {
            startActions.addAll((Collection<? extends QAction>) QConfigLoader.load(this, "onStart", section, QRegistries.ACTIONS));
            for (QAction action : startActions) {
                action.setParent(this);
            }
        }
        if (section.contains("onFinish")) {
            completeActions.addAll((Collection<? extends QAction>) QConfigLoader.load(this, "onFinish", section, QRegistries.ACTIONS));
            for (QAction action : completeActions) {
                action.setParent(this);
            }
        }
    }
}
