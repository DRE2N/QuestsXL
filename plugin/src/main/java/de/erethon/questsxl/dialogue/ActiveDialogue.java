package de.erethon.questsxl.dialogue;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author Fyreum
 */
public class ActiveDialogue extends BukkitRunnable {

    public static final long TIMER_PERIOD = 5;

    private final QPlayer qPlayer;
    private final QDialogue dialogue;
    private final Set<Integer> visitedStages = new HashSet<>();
    private int currentStageIndex;
    private QActiveDialogueStage activeStage;
    private int passedTicks;
    private int messageDelay;

    public ActiveDialogue(QPlayer qPlayer, QDialogue dialogue) {
        this.qPlayer = qPlayer;
        this.dialogue = dialogue;
        this.currentStageIndex = 0;
        this.activeStage = null; // Initialize as null, will be set in activeStage() method
        this.passedTicks = 0;
        this.messageDelay = 0;
    }

    public BukkitTask start() {
        qPlayer.setActiveDialogue(this);
        qPlayer.setInConversation(true);
        return runTaskTimerAsynchronously(QuestsXL.get(), 0, TIMER_PERIOD);
    }

    @Override
    public void run() {
        if ((passedTicks += TIMER_PERIOD) >= messageDelay) {
            continueDialogue();
        }
    }

    public void continueDialogue() {
        passedTicks = 0;
        if (activeStage == null) {
            activeStage = activeStage();
            if (activeStage == null) {
                return;
            }
        }
        try {
            messageDelay = activeStage.sendMessage(dialogue.getSenderName());
        } catch (NoSuchElementException e) {
            activeStage.onFinish();
            if (activeStage.autoNext) {
                try {
                    activeStage = nextStage();
                    if (activeStage == null || !activeStage.canStart(qPlayer)) {
                        cancel();
                    }
                } catch (IndexOutOfBoundsException e2) {
                    finish();
                }
            } else {
                if (!activeStage.dialogueOptions.isEmpty()) {
                    return;
                }
                finish();
            }
        }
    }

    public QActiveDialogueStage activeStage() {
        if (activeStage == null) {
            QDialogueStage stage = dialogue.getStages().get(currentStageIndex);
            if (stage == null) {
                finish();
                return null;
            }

            boolean isRevisited = visitedStages.contains(currentStageIndex);

            activeStage = stage.start(qPlayer);

            if (isRevisited) {
                activeStage.setRevisited(true);
            }
            visitedStages.add(currentStageIndex);
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

    public void transitionToStage(int stageIndex) {
        if (!dialogue.getStages().containsKey(stageIndex)) {
            finish();
            return;
        }

        currentStageIndex = stageIndex;
        activeStage = null;
        passedTicks = 0;
        messageDelay = 0;

        activeStage = activeStage();
        if (!activeStage.canStart(qPlayer)) {
            cancel();
            return;
        }

        continueDialogue();
    }

    private void cancel(String msg) {
        qPlayer.setActiveDialogue(null);
        qPlayer.setInConversation(false);
        qPlayer.sendMessagesInQueue(false);
        String senderName = PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(dialogue.getSenderName().get(), qPlayer.getPlayer().locale()));
        qPlayer.endDialogueAndSendRecollection(senderName);
        super.cancel();
    }

    public QDialogue getDialogue() {
        return dialogue;
    }
}
