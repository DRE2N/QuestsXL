package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class StartEventAction extends QBaseAction {


    QuestsXL plugin = QuestsXL.getInstance();

    QEvent event;
    boolean skipConditions = false;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        event.startFromAction(skipConditions);
    }

    @Override
    public void play(QEvent event) {
        if (!conditions(event)) return;
        event.startFromAction(skipConditions);
    }


    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        event = plugin.getEventManager().getByID(cfg.getString("id"));
        if (event == null) {
            throw new RuntimeException("Event " + cfg.getString("id") + " does not exist.");
        }
        skipConditions = cfg.getBoolean("skipConditions", false);
    }
}
