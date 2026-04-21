package de.erethon.questsxl.component.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.dialogue.QDialogue;
import de.erethon.questsxl.dialogue.QDialogueManager;

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

    QuestsXL plugin = QuestsXL.get();
    QDialogueManager dialogueManager = plugin.getDialogueManager();
    @QParamDoc(name = "dialogue", description = "The ID of the dialogue to play", required = true)
    QDialogue dialogue;

    @Override
    public void playInternal(Quester quester) {
        super.play(quester);
        if (!conditions(quester)) return;
        dialogue.start(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        dialogue = dialogueManager.get(cfg.getString("dialogue"));
        if (dialogue == null) {
            throw new RuntimeException("The dialogue action in " + cfg.getName() + " contains an invalid dialogue ID.");
        }
    }
}
