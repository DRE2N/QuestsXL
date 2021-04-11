package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class DelayAction extends QBaseAction {

    private transient final QuestsXL plugin = QuestsXL.getInstance();

    Set<QAction> actions;
    long delay;

    @Override
    public void play(Player player) {
        if (!conditions(player)) return;
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (QAction action : actions) {
                    action.play(player);
                }
                onFinish(player);
            }
        };
        runnable.runTaskLater(plugin, delay);
    }

    @Override
    public void load(ConfigurationSection cfg) {
        actions = ActionManager.loadActions(cfg.getConfigurationSection("actions"));
        delay = cfg.getLong("delay");
    }
}
