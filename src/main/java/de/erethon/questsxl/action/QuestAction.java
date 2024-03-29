package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;

public class QuestAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QPlayerCache playerCache = plugin.getPlayerCache();

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
    public void load(QLineConfig cfg) {
        quest = plugin.getQuestManager().getByName(cfg.getString("id"));
        if (quest == null) {
            throw new RuntimeException("Quest " + cfg.getString("id") + " does not exist.");
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        quest = plugin.getQuestManager().getByName(section.getString("id"));
        if (quest == null) {
            throw new RuntimeException("Quest " + section.getString("id") + " does not exist.");
        }
    }

}
