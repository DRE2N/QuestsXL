package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;

public class ObjectiveDisplayTextAction extends QBaseAction {

    private String text;
    private QQuest quest;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        player.getActiveQuest(quest).setObjectiveDisplayText(text);
    }

    @Override
    public void play(QEvent event) {
        if (!conditions(event)) return;
        event.setObjectiveDisplayText(text);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        text = cfg.getString("text");
        quest = QuestsXL.getInstance().getQuestManager().getByName(cfg.getString("id"));
    }
}
