package de.erethon.questsxl.dialogue;

import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.player.QPlayer;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @author Fyreum
 */
public class QActiveDialogueStage extends QDialogueStage {

    protected final QPlayer qPlayer;
    private boolean finished = false;
    private boolean isRevisited = false;

    protected QActiveDialogueStage(QPlayer qPlayer, QDialogueStage stage) {
        super(stage);
        this.qPlayer = qPlayer;
    }

    public void setRevisited(boolean isRevisited) {
        this.isRevisited = isRevisited;
    }

    public int sendMessage(QTranslatable sender) throws NoSuchElementException {
        if (isRevisited && !messages.isEmpty()) {
            // For revisited stages, show only the last message and then go directly to actions
            Map.Entry<QTranslatable, Integer> lastEntry = null;

            // Get the last message
            while (!messages.isEmpty()) {
                lastEntry = messages.pop();
            }

            if (lastEntry != null) {
                qPlayer.sendConversationMsg(lastEntry.getKey(), sender, maxMessageCount, maxMessageCount);
            }

            // Immediately show actions
            onFinish();
            return 0; // No delay for revisited stages
        } else {
            // Normal behavior for new stages
            Map.Entry<QTranslatable, Integer> entry = messages.pop();
            qPlayer.sendConversationMsg(entry.getKey(), sender, maxMessageCount - messages.size(), maxMessageCount);
            return entry.getValue();
        }
    }

    public boolean isCompleted() {
        return messages.isEmpty();
    }

    public void onFinish() {
        if (finished) {
            return;
        }
        finished = true;
        for (DialogueOption option : dialogueOptions) {
            option.show(qPlayer.getPlayer());
        }
        for (QAction postAction : actions) {
            postAction.play(qPlayer);
        }
    }
}
