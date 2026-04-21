package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.quest.QuestManager;

@QLoadableDoc(
        value = "active_quest",
        description = "Checks if the player has the specified quest active.",
        shortExample = "active_quest: quest=example_quest",
        longExample = {
                "active_quest:",
                "  quest: example_quest",
        }
)
public class ActiveQuestCondition extends QBaseCondition {

    QuestManager questManager = QuestsXL.get().getQuestManager();

    @QParamDoc(name = "quest", description = "The ID of the quest.", required = true)
    QQuest quest;

    @Override
    public boolean checkInternal(Quester quester) {
        if (quester instanceof QPlayer player) {
            if (player.hasQuest(quest)) {
                return success(quester);
            }
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        quest = questManager.getByName(cfg.getString("quest"));
    }

}
