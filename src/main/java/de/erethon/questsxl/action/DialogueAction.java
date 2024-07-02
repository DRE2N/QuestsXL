package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QBaseAction;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.dialogue.QDialogue;
import de.erethon.questsxl.dialogue.QDialogueManager;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class DialogueAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QDialogueManager dialogueManager = plugin.getDialogueManager();
    QDialogue dialogue;

    @Override
    public void play(QPlayer player) {
        super.play(player);
        if (!conditions(player)) return;
        dialogue.start(player);
    }

    @Override
    public void load(QLineConfig section) {
        dialogue = dialogueManager.get(section.getString("id"));
    }

    @Override
    public void load(ConfigurationSection section) {
        dialogue = dialogueManager.get(section.getString("id"));
    }
}
