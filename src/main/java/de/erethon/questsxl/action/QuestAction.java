package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.players.QPlayer;
import de.erethon.questsxl.players.QPlayerCache;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class QuestAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QPlayerCache playerCache = plugin.getPlayerCache();

    QQuest quest;

    @Override
    public void play(Player player) {
        if (!conditions(player)) return;
        QPlayer qPlayer = playerCache.get(player);
        if (qPlayer.hasQuest(quest)) {
            return;
        }
        qPlayer.startQuest(quest);
    }

    @Override
    public void load(String[] msg) {
        quest = plugin.getQuestManager().getByName(msg[0]);
        if (quest == null) {
            throw new RuntimeException("Quest " + msg[0] + " does not exist.");
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        quest = plugin.getQuestManager().getByName(section.getString("quest"));
        if (quest == null) {
            throw new RuntimeException("Quest " + section.getString("quest") + " does not exist.");
        }
    }

}
