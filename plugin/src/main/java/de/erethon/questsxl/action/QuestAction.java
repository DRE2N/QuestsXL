package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "quest",
        description = "Starts a quest for the player. If the player already has the quest, the action will be skipped.",
        shortExample = "- 'quest: id=example_quest'",
        longExample = {
                "quest:",
                "  id: example_quest",
        }
)
public class QuestAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QPlayerCache playerCache = plugin.getPlayerCache();

    @QParamDoc(name = "id", description = "The ID of the quest to start", required = true)
    QQuest quest;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        if (player.hasQuest(quest)) {
            return;
        }
        player.startQuest(quest);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        quest = plugin.getQuestManager().getByName(cfg.getString("id"));
        if (quest == null) {
            throw new RuntimeException("Quest " + cfg.getString("id") + " does not exist.");
        }
    }

}
