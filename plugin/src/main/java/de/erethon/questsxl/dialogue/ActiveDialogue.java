package de.erethon.questsxl.dialogue;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.NoSuchElementException;

/**
 * @author Fyreum
 */
public class ActiveDialogue extends BukkitRunnable {

    public static final long TIMER_PERIOD = 5;

    private final QPlayer qPlayer;
    private final QDialogue dialogue;
    private int currentStageIndex;
    private QActiveDialogueStage activeStage;
    private int passedTicks;
    private int messageDelay;

    public ActiveDialogue(QPlayer qPlayer, QDialogue dialogue) {
        this.qPlayer = qPlayer;
        this.dialogue = dialogue;
        this.currentStageIndex = 0;
        this.activeStage = activeStage();
        this.passedTicks = 0;
        this.messageDelay = 0;
    }

    public BukkitTask start() {
        qPlayer.setActiveDialogue(this);
        qPlayer.setInConversation(true);
        return runTaskTimerAsynchronously(QuestsXL.getInstance(), 0, TIMER_PERIOD);
    }

    @Override
    public void run() {
        if ((passedTicks += TIMER_PERIOD) >= messageDelay) {
            continueDialogue();
        }
    }

    public void continueDialogue() {
        passedTicks = 0;
        try {
            messageDelay = activeStage.sendMessage(dialogue.getSenderName());
        } catch (NoSuchElementException e) {
            activeStage.onFinish();
            try {
                activeStage = nextStage();
                if (!activeStage.canStart(qPlayer)) {
                    cancel();
                }
            } catch (IndexOutOfBoundsException e2) {
                finish();
            }
        }
    }

    public QActiveDialogueStage activeStage() {
        if (activeStage == null) {
            activeStage = dialogue.getStages().get(currentStageIndex).start(qPlayer);
        }
        return activeStage;
    }

    /**
     * @throws IndexOutOfBoundsException if there is no next stage
     */
    public QActiveDialogueStage nextStage() throws IndexOutOfBoundsException {
        activeStage = null;
        currentStageIndex++;
        return activeStage();
    }

    @Override
    public void cancel() {
        cancel("\n&7&oGespräch wurde abgebrochen\n");
    }

    public void finish() {
        cancel("\n&7&oGespräch wurde erfolgreich beendet\n");
    }

    private void cancel(String msg) {
        qPlayer.setActiveDialogue(null);
        qPlayer.setInConversation(false);
        //qPlayer.send(msg);
        qPlayer.sendMessagesInQueue(false);
        qPlayer.endDialogueAndSendRecollection(dialogue.getSenderName());
        super.cancel();
    }

    public QDialogue getDialogue() {
        return dialogue;
    }
}

