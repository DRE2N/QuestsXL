package de.erethon.questsxl.dialogue;

import de.erethon.bedrock.misc.FileUtil;
import de.erethon.bedrock.misc.Registry;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;

/**
 * @author Fyreum
 */
public class QDialogueManager extends Registry<String, QDialogue> {

    File folder;

    private final Registry<String, String> npcRegistry;

    public QDialogueManager(File folder) {
        this.folder = folder;
        this.npcRegistry = new Registry<>();
    }

    public void load() {
        for (File file : FileUtil.getFilesForFolder(folder)) {
            QDialogue dialogue = new QDialogue(file);
            add(dialogue.getName(), dialogue);
            // Only register NPC mapping if dialogue has an NPC ID
            if (dialogue.getNPCId() != null && !dialogue.getNPCId().trim().isEmpty()) {
                npcRegistry.add(dialogue.getNPCId(), dialogue.getName());
            }
        }
        QuestsXL.log("Loaded " + size() + " dialogues.");
    }

    public void onNPCRightClick(String npcId, Player player) {
        if (!npcRegistry.containsKey(npcId)) {
            return;
        }
        String dialogueId = npcRegistry.get(npcId);
        QDialogue dialogue = get(dialogueId);
        QPlayer qPlayer = QuestsXL.get().getDatabaseManager().getCurrentPlayer(player);

        // Check if player already has an active dialogue with this NPC
        boolean hadActiveDialogue = qPlayer.getActiveDialogue() != null
                && qPlayer.getActiveDialogue().getDialogue().getNPCId().equals(npcId);

        // Start new dialogue if player doesn't have one active
        if (dialogue != null && dialogue.canStartFromNPC() && !qPlayer.isInConversation() && qPlayer.getActiveDialogue() == null && dialogue.canStart(qPlayer)) {
            dialogue.start(qPlayer);
        }
        // Only continue dialogue if it was already active before this click
        else if (hadActiveDialogue) {
            qPlayer.getActiveDialogue().continueDialogue();
        }

    }

    public Registry<String, String> getNPCRegistry() {
        return npcRegistry;
    }
}
