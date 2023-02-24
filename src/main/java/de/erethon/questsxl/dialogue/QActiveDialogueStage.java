package de.erethon.questsxl.dialogue;

import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.player.QPlayer;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @author Fyreum
 */
public class QActiveDialogueStage extends QDialogueStage {

    protected final QPlayer qPlayer;

    protected QActiveDialogueStage(QPlayer qPlayer, QDialogueStage stage) {
        super(stage);
        this.qPlayer = qPlayer;
    }

    public int sendMessage(String sender) throws NoSuchElementException {
        Map.Entry<String, Integer> entry = messages.pop();
        qPlayer.sendConversationMsg(entry.getKey(), sender, maxMessageCount - messages.size(), maxMessageCount);
        return entry.getValue();
    }

    public boolean isCompleted() {
        return messages.isEmpty();
    }

    public void onFinish() {
        for (QAction postAction : postActions) {
            postAction.play(qPlayer);
        }
    }
}
