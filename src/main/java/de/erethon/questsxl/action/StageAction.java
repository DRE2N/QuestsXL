package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;

public class StageAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QPlayerCache playerCache = plugin.getPlayerCache();

    int stageID;
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
        questID = cfg.getString("quest");
        stageID = cfg.getInt("id");
    }
}
