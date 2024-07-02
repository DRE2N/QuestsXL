package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QStage;
import org.bukkit.event.Event;

public class ActiveObjective {

    private final ObjectiveHolder holder;
    private final QObjective objective;
    private final QStage stage;
    private boolean completed = false;

    public ActiveObjective(ObjectiveHolder holder, QStage stage, QObjective objective) {
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
}
