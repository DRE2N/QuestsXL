package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;

public class CompletedQuestCondition extends QBaseCondition {

    String questName;

    @Override
    public boolean check(QPlayer player) {
        for (QQuest quest : player.getCompletedQuests().keySet()) {
            if (quest.getName().equalsIgnoreCase(questName)) {
                return success(player);
            }
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
        questName = cfg.getString("quest");
    }

}
