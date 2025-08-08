package de.erethon.questsxl.quest;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QStage;
import de.erethon.questsxl.player.QPlayer;

public class ActiveQuest {

    private final QuestsXL plugin = QuestsXL.get();

    private final QPlayer player;
    private final QQuest quest;
    private QStage currentStage;

    private String objectiveDisplayText;

    public ActiveQuest(QPlayer player, QQuest quest) {
        this.player = player;
        this.quest = quest;
        QStage start = quest.getStages().get(0);
        this.currentStage = start;
        start.start(player);
        QuestsXL.log("Created new activeQuest for " + player.getPlayer().getName() + " with " + quest.getStages().size() + " stages.");
    }

    public ActiveQuest(QPlayer player, QQuest quest, int stage) {
        this.player = player;
        this.quest = quest;
        this.currentStage = quest.getStages().get(stage);
        QuestsXL.log("Loaded already started activeQuest for " + player.getPlayer().getName() + " with " + quest.getStages().size() + " stages.");
    }

    public void progress(QPlayer player) {
        QuestsXL.log("Progressing quest " + quest.getName() + " for " + player.getPlayer().getName());
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
        currentStage.start(player);
        plugin.debug(player.getPlayer().getName() + " progressed to stage " + currentStage.toString() + " of " + quest.getName() + ".");
    }

    public void finish(QPlayer player) {
        quest.reward(player);
        QuestsXL.log(player.getPlayer().getName() + " finished quest " + quest.getName());
        player.clearObjectives();
        player.removeActive(this);
        player.getCompletedQuests().put(this.getQuest(), System.currentTimeMillis());
    }

    public void finishWithoutRewards(QPlayer player) {
        QuestsXL.log(player.getPlayer().getName() + " finished quest " + quest.getName() + " without rewards.");
        player.clearObjectives();
        player.removeActive(this);
        player.getCompletedQuests().put(this.getQuest(), System.currentTimeMillis());
    }

    public QStage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(QStage currentStage) {
        player.clearObjectives();
        this.currentStage = currentStage;
        currentStage.start(player);
    }

    public QPlayer getPlayer() {
        return player;
    }

    public QQuest getQuest() {
        return quest;
    }

    public String getObjectiveDisplayText() {
        return objectiveDisplayText;
    }

    public void setObjectiveDisplayText(String objectiveDisplayText) {
        this.objectiveDisplayText = objectiveDisplayText;
    }
}
