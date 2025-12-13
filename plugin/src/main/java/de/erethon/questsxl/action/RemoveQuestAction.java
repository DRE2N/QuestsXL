package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;

@QLoadableDoc(
        value = "remove_quest",
        description = "Removes a quest from the player. This will delete all progress made on the quest.",
        shortExample = "remove_quest: quest=example_quest",
        longExample = {
                "remove_quest:",
                "  quest: example_quest",
        }
)
public class RemoveQuestAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.get();

    @QParamDoc(name = "quest", description = "The ID of the quest to remove", required = true)
    String questId;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester, this::removeQuest);
        onFinish(quester);
    }

    private void removeQuest(QPlayer player) {
        QQuest quest = plugin.getQuestManager().getByName(questId);
        if (quest == null) {
            throw new RuntimeException("Quest " + questId + " does not exist. (RemoveQuestAction in " + id() + ")");
        }
        if (player.hasQuest(quest)) {
            return;
        }
        player.removeQuest(quest);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        questId = cfg.getString("quest");
    }
}
