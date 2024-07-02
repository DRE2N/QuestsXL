package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
enum RUN_MODE {
    EVENT_IN_RANGE,
    EVENT_PARTICIPANTS,
    ONLINE,
}
public class RunAsAction extends QBaseAction {

    private RUN_MODE runMode = RUN_MODE.ONLINE;
    private int runValue;
    private Set<QAction> actions;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        run(player); // This is useless but whatever
        onFinish(player);
    }

    @Override
    public void play(QEvent event) {
        if (!conditions(event)) return;
        switch (runMode) {
            case EVENT_IN_RANGE:
                event.getPlayersInRange().forEach(this::run);
                break;
            case EVENT_PARTICIPANTS:
                for (Map.Entry<QPlayer, Integer> entry : event.getParticipants().entrySet()) {
                    if (entry.getValue() >= runValue) {
                        run(entry.getKey());
                    }
                }
                break;
            case ONLINE:
                for (Player player : Bukkit.getOnlinePlayers()) {
                    run(cache.getByPlayer(player));
                }
                break;
        }
        onFinish(event);
    }

    private void run(QPlayer player)  {
        for (QAction action : actions) {
            action.play(player);
        }
    }

    @Override
    public void load(QLineConfig cfg) {
        throw new UnsupportedOperationException("RunAsAction does not support single-line configs.");
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        runMode = RUN_MODE.valueOf(section.getString("mode", "online").toUpperCase());
        runValue = section.getInt("value", 0);
        try {
            actions = (Set<QAction>) QConfigLoader.load("actions", section, QRegistries.ACTIONS);
            if (actions.isEmpty()) {
                throw new RuntimeException("Action list is empty.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load actions: " + e.getMessage());
        }
    }

}

