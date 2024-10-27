package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
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
    QPlayerCache playerCache = plugin.getPlayerCache();

    @QParamDoc(name = "id", description = "The quest or event to change the stage of", required = true)
    int stageID;
    @QParamDoc(name = "stage", description = "The stage to set", required = true)
    String questID;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        QQuest quest = plugin.getQuestManager().getByName(questID);
        if (!player.hasQuest(quest)) {
            return;
        }
        ActiveQuest active = player.getActive(quest.getName());
        if (active == null) {
            return;
        }
        active.setCurrentStage(quest.getStages().get(stageID));
        MessageUtil.log("Forced stage " + stageID + " for " + player.getPlayer().getName() + " in " + quest.getName());
        onFinish(player);
    }

    @Override
    public void play(QEvent event) {
        if (!conditions(event)) return;
        event.setCurrentStage(stageID);
        MessageUtil.log("Forced stage " + stageID + " for " + event.getName());
        onFinish(event);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        questID = cfg.getString("id");
        stageID = cfg.getInt("stage");
    }
}
