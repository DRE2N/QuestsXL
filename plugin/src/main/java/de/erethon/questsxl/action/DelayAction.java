package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
@QLoadableDoc("delay")
public class DelayAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();

    Set<QAction> actions;
    @QParamDoc("Duration in seconds")
    long delay;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        List<QAction> tmp = new ArrayList<>(actions);
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (QAction action : tmp) {
                    action.play(player);
                }
                onFinish(player);
            }
        };
        runnable.runTaskLater(plugin, delay * 20);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        actions =  cfg.getActions("actions");
        delay = cfg.getLong("duration");
        if (actions.isEmpty()) {
            throw new RuntimeException("Action list is empty.");
        }
    }
}
