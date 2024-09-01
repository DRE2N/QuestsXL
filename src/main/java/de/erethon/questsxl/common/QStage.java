package de.erethon.questsxl.common;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class QStage {

    private final Completable owner;
    private final Set<QObjective> goals = new HashSet<>();
    private final Set<QCondition> conditions = new HashSet<>();
    private final Set<QAction> startActions = new HashSet<>();
    private final Set<QAction> completeActions = new HashSet<>();
    private final int id;
    private String startMessage = "";
    private String description = "";
    private String completeMessage = "";

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
        MessageUtil.log("Starting stage " + id);
        for (QObjective objective : goals) {
            MessageUtil.log("Added objective " + objective.getClass().getName());
            holder.addObjective(new ActiveObjective(holder, getQuest(), this, objective));
        }
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

    /** Checks for completions. This is called every time an objective is completed.
     * @param holder the holder that completed the objective.
     */
    public void checkCompleted(ObjectiveHolder holder) {
       if (isCompleted(holder)) {
           MessageUtil.log("Stage is completed!");
           holder.progress(owner);
           return;
       }
       MessageUtil.log("Stage not completed");
    }

    /**
     * @deprecated Currently unused.
     * @param player the player to check
     * @return true if the player can start this stage, false otherwise.
     */
    @Deprecated
    public boolean canStart(QPlayer player) {
        Set<QCondition> failed = new HashSet<>();
        boolean canStart = true;
        for (QCondition condition : conditions) {
            if (!condition.check(player)) {
                canStart = false;
                failed.add(condition);
            }
        }
        player.send(failed.toString());
        return canStart;
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

    public void load(ConfigurationSection section) {
        startMessage = section.getString("startMessage", "");
        completeMessage = section.getString("completeMessage", "");
        description = section.getString("description", "");
        if (section.contains("conditions")) {
            conditions.addAll((Collection<? extends QCondition>) QConfigLoader.load("conditions", section, QRegistries.CONDITIONS));
        }
        goals.addAll((Collection<? extends QObjective>) QConfigLoader.load("objectives", section, QRegistries.OBJECTIVES));
        if (section.contains("onStart")) {
            startActions.addAll((Collection<? extends QAction>) QConfigLoader.load("onStart", section, QRegistries.ACTIONS));
        }
        if (section.contains("onFinish")) {
            completeActions.addAll((Collection<? extends QAction>) QConfigLoader.load("onFinish", section, QRegistries.ACTIONS));
        }
    }
}
