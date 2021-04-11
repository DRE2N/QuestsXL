package de.erethon.questsxl.condition;

import de.erethon.questsxl.players.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class QuestCondition extends QBaseCondition {

    String questName;

    @Override
    public boolean check(QPlayer player) {
        for (QQuest quest : player.getCompletedQuests().keySet()) {
            if (quest.getName().equalsIgnoreCase(questName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        questName = section.getString("quest");
    }

}
