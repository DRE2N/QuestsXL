package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class StageAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QPlayerCache playerCache = plugin.getPlayerCache();

    int stageID;
    String questID;

    @Override
    public void play(Player player) {
        if (!conditions(player)) return;
        QPlayer qPlayer = playerCache.getByPlayer(player);
        QQuest quest = plugin.getQuestManager().getByName(questID);
        if (!qPlayer.hasQuest(quest)) {
            return;
        }
        ActiveQuest active = qPlayer.getActive(quest.getName());
        if (active == null) {
            return;
        }
        active.setCurrentStage(quest.getStages().get(stageID));
        MessageUtil.log("Forced stage " + stageID + " for " + player.getName() + " in " + quest.getName());
        onFinish(player);
    }

    @Override
    public void load(String[] msg) {
        questID = msg[0];
        stageID = Integer.parseInt(msg[1]);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        questID = section.getString("quest");
        stageID = section.getInt("id");
    }
}
