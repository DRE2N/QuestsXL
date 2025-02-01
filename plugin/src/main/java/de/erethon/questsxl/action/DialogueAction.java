package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QBaseAction;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.dialogue.QDialogue;
import de.erethon.questsxl.dialogue.QDialogueManager;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "play_dialogue",
        description = "Starts playing a dialogue. The dialogue needs to be defined in a separate file in. \nIf the dialogue is not found, the action will fail on load.",
        shortExample = "play_dialogue: dialogue=example_dialogue",
        longExample = {
                "dialogue:",
                "  id: example_dialogue",
        }
)
public class DialogueAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QDialogueManager dialogueManager = plugin.getDialogueManager();
    @QParamDoc(name = "dialogue", description = "The ID of the dialogue to play", required = true)
    QDialogue dialogue;

    @Override
    public void play(Quester quester) {
        super.play(quester);
        if (!conditions(quester)) return;
        dialogue.start(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        dialogue = dialogueManager.get(cfg.getString("dialogue"));
    }
}
