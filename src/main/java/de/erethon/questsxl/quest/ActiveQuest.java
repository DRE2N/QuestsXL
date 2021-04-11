package de.erethon.questsxl.quest;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.objectives.ActiveObjective;
import de.erethon.questsxl.objectives.QObjective;
import de.erethon.questsxl.players.QPlayer;

public class ActiveQuest {

    QuestsXL plugin = QuestsXL.getInstance();

    QPlayer player;
    QQuest quest;
    QStage currentStage;

    public ActiveQuest(QPlayer player, QQuest quest) {
        this.player = player;
        this.quest = quest;
        QStage start = quest.getStages().get(0);
        this.currentStage = start;
        start.start(player);
        MessageUtil.log("Created new activeQuest for " + player.getPlayer().getName() + " with " + start.getGoals().size() + " objectives.");
    }

    public void progress(QPlayer player) {
        MessageUtil.log("Progressing...");
        QStage next = null;
        int currentID = currentStage.getId();
        for (QStage stage : quest.getStages()) {
            if (stage.getId() == currentID + 1) {
                next = stage;
            }
        }
        if (next == null) {
            finish(player);
            return;
        }
        currentStage = next;
        player.clearObjectives();
        for (QObjective objective : currentStage.getGoals()) {
            player.addObjective(new ActiveObjective(player, currentStage, objective));
        }
        plugin.debug(player.getPlayer().getName() + " progressed to stage " + currentStage.toString() + " of " + quest.getName() + ".");
    }

    public void finish(QPlayer player) {
        quest.reward(player);
        MessageUtil.log(player.getPlayer().getName() + " finished quest " + quest.getName());
        player.clearObjectives();
        player.removeActive(this);
        player.getCompletedQuests().put(this.getQuest(), System.currentTimeMillis());
    }

    public void finishWithoutRewards(QPlayer player) {
        MessageUtil.log(player.getPlayer().getName() + " finished quest " + quest.getName());
        player.clearObjectives();
        player.removeActive(this);
        player.getCompletedQuests().put(this.getQuest(), System.currentTimeMillis());
    }

    public QStage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(QStage currentStage) {
        this.currentStage = currentStage;
    }

    public QPlayer getPlayer() {
        return player;
    }

    public QQuest getQuest() {
        return quest;
    }
}
