package de.erethon.questsxl.objective;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.Completable;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.QStage;
import de.erethon.questsxl.global.GlobalObjectives;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

public class ActiveObjective {

    private final ObjectiveHolder holder;
    private final QObjective objective;
    private final QStage stage;
    private boolean completed = false;
    private int progress = 0;

    public ActiveObjective(ObjectiveHolder holder, Completable completable, QStage stage, QObjective objective) {
        this.holder = holder;
        this.objective = objective;
        this.stage = stage;
        objective.onStart(holder);
    }

    public void check(Event event) {
        objective.check(this, event);
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public ObjectiveHolder getHolder() {
        return holder;
    }

    public String getMessage() {
        return objective.getDisplayText();
    }

    public QObjective getObjective() {
        return objective;
    }

    public boolean isCompleted() {
        return completed;
    }

    public QStage getStage() {
        return stage;
    }

    public void addProgress(int progress) {
        this.progress += progress;
    }

    public void removeProgress(int progress) {
        this.progress -= progress;
        if (this.progress < 0) {
            this.progress = 0;
        }
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void save(ConfigurationSection section) {
        section.set("objective", QRegistries.OBJECTIVES.getId(objective.getClass()));
        section.set("completed", completed);
        section.set("progress", progress);
        if (stage.getOwner() instanceof QEvent event) {
            section.set("event", event.getId());
        }
        if (stage.getOwner() instanceof QQuest quest) {
            section.set("quest", quest.getName());
        }
        if (stage.getOwner() instanceof GlobalObjectives global) {
            section.set("global", true);
        }
        section.set("stage", stage.getId());
    }

    public static ActiveObjective load(ObjectiveHolder holder, ConfigurationSection section) {
        String objectiveId = section.getString("objective");
        QObjective objective = QRegistries.OBJECTIVES.get(objectiveId);
        if (objective == null) {
            QuestsXL.getInstance().getLogger().warning("Failed to load objective with id " + objectiveId + " Missing dependency?");
            return null;
        }
        boolean completed = section.getBoolean("completed");
        int progress = section.getInt("progress");
        Completable completable = null;
        if (section.contains("event")) {
            completable = QuestsXL.getInstance().getEventManager().getByID(section.getString("event"));
        }
        if (section.contains("quest")) {
            completable = QuestsXL.getInstance().getQuestManager().getByName(section.getString("quest"));
        }
        if (section.contains("global")) {
            completable = QuestsXL.getInstance().getGlobalObjectives();
        }
        QStage stage = completable.getStages().get(section.getInt("stage"));

        ActiveObjective activeObjective = new ActiveObjective(holder, completable, stage, objective);
        activeObjective.setCompleted(completed);
        activeObjective.setProgress(progress);
        return activeObjective;
    }

}
