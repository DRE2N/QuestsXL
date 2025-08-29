package de.erethon.questsxl.dialogue;

import de.erethon.bedrock.misc.FileUtil;
import de.erethon.bedrock.misc.Registry;
import de.erethon.questsxl.QuestsXL;

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

    public Registry<String, String> getNPCRegistry() {
        return npcRegistry;
    }
}
