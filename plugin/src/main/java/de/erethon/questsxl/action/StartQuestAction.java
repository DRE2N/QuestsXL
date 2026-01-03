package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;

@QLoadableDoc(
        value = "start_quest",
        description = "Starts a quest for the player. If the player already has the quest, the action will be skipped.",
        shortExample = "start_quest: quest=example_quest",
        longExample = {
                "start_quest:",
                "  quest: example_quest",
        }
)
public class StartQuestAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.get();

    @QParamDoc(name = "quest", description = "The ID of the quest to start", required = true)
    String questId;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester, this::startQuest);
        onFinish(quester);
    }

    private void startQuest(QPlayer player) {
        QQuest quest = plugin.getQuestManager().getByName(questId);
        if (quest == null) {
            throw new RuntimeException("Quest " + questId + " does not exist. (QuestAction in " + id() + ")");
        }
        if (player.hasQuest(quest)) {
            return;
        }
        player.startQuest(quest);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        questId = cfg.getString("quest");
    }

}
