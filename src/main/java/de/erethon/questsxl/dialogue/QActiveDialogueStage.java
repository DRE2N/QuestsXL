package de.erethon.questsxl.dialogue;

import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.player.QPlayer;

import java.util.Map;

/**
 * @author Fyreum
 */
public class QActiveDialogueStage extends QDialogueStage {

    protected final QPlayer qPlayer;

    protected QActiveDialogueStage(QPlayer qPlayer, QDialogueStage stage) {
        super(stage);
        this.qPlayer = qPlayer;
    }

    public int sendMessage(String sender) throws NullPointerException {
        Map.Entry<String, Integer> entry = messages.pollFirstEntry();
        if (entry == null) {
            throw new NullPointerException("No messages left");
        }
        qPlayer.sendConversationMsg("&5" + sender + "&8: &7" + entry.getKey());
        return entry.getValue();
    }

    public boolean isCompleted() {
        return messages.isEmpty();
    }

    public void onFinish() {
        for (QAction postAction : postActions) {
            postAction.play(qPlayer.getPlayer());
        }
    }
}
