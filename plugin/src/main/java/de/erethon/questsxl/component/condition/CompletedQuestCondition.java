package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;

@QLoadableDoc(
        value = "completed_quest",
        description = "Checks if the player has the specified quest completed.",
        shortExample = "completed_quest: quest=example_quest",
        longExample = {
                "completed_quest:",
                "  quest: example_quest",
        }
)
public class CompletedQuestCondition extends QBaseCondition {

    @QParamDoc(name = "quest", description = "The name of the quest that the player must have completed.", required = true)
    String questName;

    @Override
    public boolean checkInternal(Quester quester) {
        if (quester instanceof QPlayer player) {
            for (QQuest quest : player.getCompletedQuests().keySet()) {
                if (quest.getName().equalsIgnoreCase(questName)) {
                    return success(quester);
                }
            }
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        questName = cfg.getString("quest");
    }

}
