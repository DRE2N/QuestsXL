package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class RepeatAction extends QBaseAction {

    private transient final QuestsXL plugin = QuestsXL.getInstance();

    Set<QAction> actions;
    long delay;
    int repetitions;
    int current = 0;

    BukkitRunnable task;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        cancel();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (QAction action : actions) {
                    action.play(player);
                }
                if (current >= repetitions) {
                    onFinish(player);
                    cancel();
                }
                current++;
            }
        };
        task.runTaskTimer(plugin, delay, delay);
    }

    @Override
    public void play(QEvent event) {
        if (!conditions(event)) return;
        cancel();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (QAction action : actions) {
                    action.play(event);
                }
                if (current >= repetitions) {
                    onFinish(event);
                    cancel();
                }
                current++;
            }
        };
        task.runTaskTimer(plugin, delay, delay);
    }

    public void cancel() {
        if (task != null) {
            current = 0;
            task.cancel();
        }
    }

    @Override
    public void load(QLineConfig cfg) {
        throw new UnsupportedOperationException("RepeatAction does not support single-line configs");
    }

    @Override
    public void load(ConfigurationSection cfg) {
        actions = (Set<QAction>) QConfigLoader.load("actions", cfg, QRegistries.ACTIONS);
        delay = cfg.getLong("delay");
        repetitions = cfg.getInt("repetitions") - 1;
    }
}
