package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "stage",
        description = "Changes the current stage of a quest or event. This is a powerful actin that can be used for branching quests or events. " +
                "\nFor example, you could create a dialogue that gives the player a choice, and depending on the choice, you could set a different stage.",
        shortExample = "stage: id=example_quest; stage=1",
        longExample = {
                "stage:",
                "  id: example_event",
                "  stage: 99"
        }
)
public class StageAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    @QParamDoc(name = "id", description = "The quest or event to change the stage of. Not required when run from event")
    String questID;
    @QParamDoc(name = "stage", description = "The stage to set", required = true)
    int stageID;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        if (quester instanceof QPlayer player) {
            QQuest quest = plugin.getQuestManager().getByName(questID);
            if (!player.hasQuest(quest)) {
                return;
            }
            ActiveQuest active = player.getActive(quest.getName());
            if (active == null) {
                return;
            }
            active.setCurrentStage(quest.getStages().get(stageID));
        }
        if (quester instanceof QEvent event) {
            QEvent event1 = plugin.getEventManager().getByID(questID);
            if (event1 == null) { // If no event is provided, use self
                event.setCurrentStage(stageID);
                return;
            }
            event1.setCurrentStage(stageID);
        }
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        questID = cfg.getString("id");
        stageID = cfg.getInt("stage");
    }
}
