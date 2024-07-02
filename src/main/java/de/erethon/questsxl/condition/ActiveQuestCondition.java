package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.quest.QuestManager;
import org.bukkit.configuration.ConfigurationSection;

public class ActiveQuestCondition extends QBaseCondition {

    QuestManager questManager = QuestsXL.getInstance().getQuestManager();

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
    public void load(ConfigurationSection section) {
        quest = questManager.getByName(section.getString("id"));
    }

    @Override
    public void load(QLineConfig section) {
        quest = questManager.getByName(section.getString("id"));
    }
}
