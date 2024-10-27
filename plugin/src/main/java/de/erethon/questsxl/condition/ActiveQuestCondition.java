package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.quest.QuestManager;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "active_quest",
        description = "Checks if the player has the specified quest active.",
        shortExample = "'active_quest: id=example_quest'",
        longExample = {
                "active_quest:",
                "  id: example_quest",
        }
)
public class ActiveQuestCondition extends QBaseCondition {

    QuestManager questManager = QuestsXL.getInstance().getQuestManager();

    @QParamDoc(name = "id", description = "The ID of the quest.", required = true)
    QQuest quest;

    @Override
    public boolean check(QPlayer player) {
        if (player.hasQuest(quest)) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public boolean check(QEvent event) {
        return fail(event);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        quest = questManager.getByName(cfg.getString("id"));
    }

}
