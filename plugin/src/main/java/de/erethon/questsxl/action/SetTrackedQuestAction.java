package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;

@QLoadableDoc(
        value = "set_tracked_quest",
        description = "Sets the quest that the player is currently tracking (e.g. shown in the sidebar). Priority is used to determine which event is shown if multiple actions are active. Quest has to be active.",
        shortExample = "set_tracked_event: event=example_event",
        longExample = {
                "set_tracked_quest:",
                "  quest: example_event",
                "  priority: 7"
        }
)
public class SetTrackedQuestAction extends QBaseAction {

    @QParamDoc(name = "quest", description = "ID of the quest to set as the tracked quest.", required = true)
    private QQuest quest;
    @QParamDoc(name = "priority", description = "Priority. Higher values equal a higher priority", def = "1")
    private int priority = 0;


    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester, (QPlayer player) -> player.setTrackedQuest(quest, priority));
        onFinish(quester);
    }


    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        quest = cfg.getQuest("event");
        if (quest == null) {
            QuestsXL.getInstance().addRuntimeError(new FriendlyError(cfg.getName(), "Quest not found", "The quest specified in the action could not be found.", "Check the quest name in the action configuration."));
        }
        priority = cfg.getInt("priority", 1);
    }
}
