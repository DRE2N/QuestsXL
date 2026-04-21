package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.quest.QuestManager;

import java.util.Map;

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
public class StageCondition extends QBaseCondition implements VariableProvider {

    QuestManager questManager = QuestsXL.get().getQuestManager();

    @QParamDoc(name = "quest", description = "The ID of the quest.", required = true)
    String questId;

    @QParamDoc(name = "stage", description = "The stage the quest must be on.", required = true)
    private int stage = -1;

    private int lastStage = -1;

    @Override
    public boolean checkInternal(Quester quester) {
        QQuest quest = questManager.getByName(questId);
        if (quest == null) {
            throw new RuntimeException("Quest " + questId + " does not exist. (StageCondition in " + id() + ")");
        }
        if (quester instanceof QPlayer player) {
            if (player.hasQuest(quest)) {
                lastStage = player.getActiveQuest(quest).getCurrentStageIndex();
                if (lastStage == stage) {
                    return success(quester);
                }
            }
        }
        return fail(quester);
    }

    /** Exposes %stage% (the player's actual current stage index) to child actions. */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("stage", new QVariable(lastStage));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        questId = cfg.getString("quest");
        stage = cfg.getInt("stage");
    }
}
