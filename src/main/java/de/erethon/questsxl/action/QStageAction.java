package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.players.QPlayer;
import de.erethon.questsxl.players.QPlayerCache;
import de.erethon.questsxl.quest.ActiveQuest;
import org.bukkit.entity.Player;

public class QStageAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QPlayerCache playerCache = plugin.getPlayerCache();

    int stageID;
    ActiveQuest quest;

    @Override
    public void play(Player player) {
        if (!conditions(player)) return;
        QPlayer qPlayer = playerCache.get(player);
        if (!qPlayer.getActiveQuests().containsKey(quest)) {
            return;
        }
        quest.setCurrentStage(quest.getQuest().getStages().get(stageID));
        onFinish(player);
    }
}
