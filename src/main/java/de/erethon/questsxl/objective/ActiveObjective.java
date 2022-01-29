package de.erethon.questsxl.objective;

import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QStage;
import org.bukkit.event.Event;

public class ActiveObjective {

    private final QPlayer player;
    private final QObjective objective;
    private final QStage stage;
    private boolean completed = false;

    public ActiveObjective(QPlayer player, QStage stage,  QObjective objective) {
        this.player = player;
        this.objective = objective;
        this.stage = stage;
        objective.onStart(player);
    }

    public void check(Event event) {
        objective.check(event);
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public QPlayer getPlayer() {
        return player;
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
