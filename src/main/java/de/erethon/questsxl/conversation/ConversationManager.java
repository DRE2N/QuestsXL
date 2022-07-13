package de.erethon.questsxl.conversation;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.livingworld.QEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ConversationManager {

    Set<QConversation> conversations = new HashSet<>();

    public ConversationManager() {
    }

    public QConversation getByID(String id) {
        return conversations.stream().filter(conversation -> id.equals(conversation.getID())).findFirst().orElse(null);
    }

    public void load(File file) {
        for (File file1 : file.listFiles()) {
            conversations.add(new QConversation(file1));
        }
        for (QConversation conversation : conversations) {
            conversation.load();
        }
        MessageUtil.log("Loaded " + conversations.size() + " conversations.");
    }

    public void save() {
        for (QConversation conversation : conversations) {
            try {
                conversation.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
