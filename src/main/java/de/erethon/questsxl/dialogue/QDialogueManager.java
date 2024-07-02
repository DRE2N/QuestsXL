package de.erethon.questsxl.dialogue;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.FileUtil;
import de.erethon.bedrock.misc.Registry;

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
            npcRegistry.add(dialogue.getNPCId(), dialogue.getName());
        }
        MessageUtil.log("Loaded " + size() + " dialogues.");
    }

    public Registry<String, String> getNPCRegistry() {
        return npcRegistry;
    }
}
