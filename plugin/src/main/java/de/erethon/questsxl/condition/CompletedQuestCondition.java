package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;

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
    public boolean check(Quester quester) {
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
