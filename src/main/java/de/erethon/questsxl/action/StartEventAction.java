package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
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
    public void load(String[] msg) {
        event = plugin.getEventManager().getByID(msg[0]);
        if (event == null) {
            throw new RuntimeException("Event " + msg[0] + " does not exist.");
        }
        skipConditions = Boolean.parseBoolean(msg[1]);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        event = plugin.getEventManager().getByID(section.getString("id"));
        if (event == null) {
            throw new RuntimeException("Event " + section.getString("id") + " does not exist.");
        }
        skipConditions = section.getBoolean("skipConditions", false);
    }
}
