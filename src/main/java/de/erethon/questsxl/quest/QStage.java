package de.erethon.questsxl.quest;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.action.ActionManager;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.condition.ConditionManager;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.ObjectiveManager;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

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

    public QStage(Completable quest,  int id) {
        this.owner = quest;
        this.id = id;
    }

    public void start(QPlayer qPlayer) {
        for (QObjective objective : goals) {
            qPlayer.addObjective(new ActiveObjective(qPlayer, this, objective));
        }
        for (QAction action : startActions) {
            action.play(qPlayer.getPlayer());
        }
    }

    public void start(QEvent event) {
        for (QAction action : startActions) {
            action.play(event);
        }
    }

    // gets called whenever an objective is completed
    public void checkCompleted(QPlayer player) {
       if (isCompleted(player)) {
           MessageUtil.log("Stage is completed!");
           player.progress(owner);
           return;
       }
       MessageUtil.log("Stage not completed");
    }

    // Checked before the stage gets started
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

    public boolean isCompleted(QPlayer player) {
        for (ActiveObjective activeObjective : player.getCurrentObjectives()) {
            if (activeObjective.getStage() == null) {
                continue;
            }
            if (goals.contains(activeObjective.getObjective())) {
                if (!activeObjective.isCompleted() && !activeObjective.getObjective().isOptional()) {
                    return false;
                }
            }
        }
        return true;
    }


    public boolean hasObjective(QObjective obj) {
        return goals.contains(obj);
    }

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
        startMessage = section.getString("startMessage");
        completeMessage = section.getString("completeMessage");
        description = section.getString("description");
        if (section.getConfigurationSection("conditions") != null) {
            conditions.addAll(ConditionManager.loadConditions(this.owner.getName() + " - " + id + ": conditions", section.getConfigurationSection("conditions")));
        }
        goals.addAll(ObjectiveManager.loadObjectives(this.owner.getName() + " - " + id + ": objectives", section.getConfigurationSection("objectives")));
        if (section.contains("onStart")) {
            startActions.addAll(ActionManager.loadActions(this.owner.getName() + " - " + id + ": onStart", section.getConfigurationSection("onStart")));
        }
        if (section.contains("onFinish")) {
            completeActions.addAll(ActionManager.loadActions(this.owner.getName() + " - " + id + ": onFinish", section.getConfigurationSection("onFinish")));
        }
    }
}
