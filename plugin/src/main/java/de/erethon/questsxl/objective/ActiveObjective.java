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

import java.util.Objects;

public class ActiveObjective {

    private final ObjectiveHolder holder;
    private final QObjective<?> objective;
    private final QStage stage;
    private final Completable completable;
    private boolean completed = false;
    private int progress = 0;

    public ActiveObjective(ObjectiveHolder holder, Completable completable, QStage stage, QObjective<?> objective) {
        this.holder = holder;
        this.objective = objective;
        this.stage = stage;
        this.completable = completable;
        objective.onStart(holder);
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public ObjectiveHolder getHolder() {
        return holder;
    }

    public QObjective<?> getObjective() {
        return objective;
    }

    public boolean isCompleted() {
        return completed;
    }

    public QStage getStage() {
        return stage;
    }

    public Completable getCompletable() {
        return completable;
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

    public void saveToDatabase() {
        // Skip database operations for objectives without a stage (e.g., WorldInteraction objectives)
        if (stage == null) {
            return;
        }
        var databaseManager = QuestsXL.get().getDatabaseManager();
        if (databaseManager != null) {
            databaseManager.saveObjectiveProgress(this);
        }
    }

    public void removeFromDatabase() {
        // Skip database operations for objectives without a stage (e.g., WorldInteraction objectives)
        if (stage == null) {
            return;
        }
        var databaseManager = QuestsXL.get().getDatabaseManager();
        if (databaseManager != null) {
            databaseManager.removeObjectiveProgress(this);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveObjective that = (ActiveObjective) o;
        return getHolder().getUniqueId().equals(that.getHolder().getUniqueId()) && objective.equals(that.objective);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHolder().getUniqueId(), objective);
    }

}
