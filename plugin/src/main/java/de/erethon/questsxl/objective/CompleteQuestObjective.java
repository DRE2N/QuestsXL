package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.event.QQuestCompleteEvent;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "complete_quest",
        description = "Completed when a specific quest is completed.",
        shortExample = "complete_quest: quest=example_quest",
        longExample = {
                "complete_quest:",
                "  quest: 'example_quest'"
        }
)
public class CompleteQuestObjective extends QBaseObjective<QQuestCompleteEvent> {

    @QParamDoc(name = "quest", description = "The ID of the quest that needs to be completed to complete this objective.", required = true)
    private String questID;

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("de=Schließe Quest " + questID + " ab; en=Complete quest " + questID);
    }

    @Override
    public Class<QQuestCompleteEvent> getEventType() {
        return QQuestCompleteEvent.class;
    }

    @Override
    public void check(ActiveObjective activeObjective, QQuestCompleteEvent event) {
        if (!conditions(event.getPlayer())) {
            return;
        }
        if (questID != null && !event.getQuest().id().equals(questID)) {
            return;
        }
        complete(event.getQPlayer(), this, event.getQPlayer());
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        if (cfg.contains("quest")) {
            questID = cfg.getString("quest");
        }
        if (questID == null) {
            plugin.addRuntimeError(new FriendlyError(findTopParent().id(), "Invalid quest objective", "No quest ID specified.", "Add a valid quest ID."));
        }
    }
}
