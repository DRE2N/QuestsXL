package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.quest.QuestManager;

@QLoadableDoc(
        value = "stage",
        description = "Checks if the player is on a specific stage of a quest.",
        shortExample = "stage: quest=example_quest stage=2",
        longExample = {
                "stage:",
                "  quest: example_quest",
                "  stage: 2",
        }
)
public class StageCondition extends QBaseCondition {

    QuestManager questManager = QuestsXL.get().getQuestManager();

    @QParamDoc(name = "quest", description = "The ID of the quest.", required = true)
    String questId;

    @QParamDoc(name = "stage", description = "The stage the quest must be on.", required = true)
    private int stage = -1;

    @Override
    public boolean check(Quester quester) {
        QQuest quest = questManager.getByName(questId);
        if (quest == null) {
            throw new RuntimeException("Quest " + questId + " does not exist. (StageCondition in " + id() + ")");
        }
        if (quester instanceof QPlayer player) {
            if (player.hasQuest(quest) && player.getActiveQuest(quest).getCurrentStageIndex() == stage) {
                return success(quester);
            }
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        questId = cfg.getString("quest");
        stage = cfg.getInt("stage");
    }
}
