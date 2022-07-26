package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
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

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (QAction action : actions) {
                    MessageUtil.log("Playing repeated action..");
                    action.play(player);
                }
                if (current >= repetitions) {
                    onFinish(player);
                    cancel();
                }
                current++;
            }
        };
        runnable.runTaskTimer(plugin, delay, delay);
    }

    @Override
    public void play(QEvent event) {
        if (!conditions(event)) return;
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (QAction action : actions) {
                    MessageUtil.log("Playing repeated action..");
                    action.play(event);
                }
                if (current >= repetitions) {
                    onFinish(event);
                    cancel();
                }
                current++;
            }
        };
        runnable.runTaskTimer(plugin, delay, delay);
    }

    @Override
    public void load(ConfigurationSection cfg) {
        actions = ActionManager.loadActions(cfg.getName() + ": Repeat action", cfg.getConfigurationSection("actions"));
        delay = cfg.getLong("delay");
        repetitions = cfg.getInt("repetitions");
    }
}
