package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

@QLoadableDoc(
        value = "repeat",
        description = "Repeats a set of actions a specified amount of times with a delay between each repetition.",
        shortExample = "<no short syntax>",
        longExample = {
                "repeat:",
                "  delay: 10",
                "  repetitions: 5",
                "  actions:",
                "    - 'message: message=Hello world'"
        }
)
public class RepeatAction extends QBaseAction {

    private transient final QuestsXL plugin = QuestsXL.getInstance();

    @QParamDoc(name = "delay", description = "The delay between each repetition in seconds", def="0")
    long delay;
    @QParamDoc(name = "repetitions", description = "The amount of repetitions", def = "1")
    int repetitions;
    @QParamDoc(name = "actions", description = "The list of actions to repeat", required = true)
    Set<QAction> actions;

    int current = 0;

    BukkitRunnable task;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        cancel();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (QAction action : actions) {
                    try {
                        action.play(quester);
                    } catch (Exception e) {
                        FriendlyError error = new FriendlyError(id,"Failed to execute repeat actions", e.getMessage(), "Player: " + quester.getName()).addStacktrace(e.getStackTrace());
                        plugin.addRuntimeError(error);
                    }
                }
                if (current >= repetitions) {
                    onFinish(quester);
                    cancel();
                }
                current++;
            }
        };
        task.runTaskTimer(plugin, delay, delay);
    }

    @Override
    public void cancel() {
        if (task != null) {
            current = 0;
            task.cancel();
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        actions = cfg.getActions(this, "actions");
        for (QAction action : actions) {
           action.setParent(this);
        }
        delay = cfg.getLong("delay", 0);
        repetitions = cfg.getInt("repetitions", 1);
    }
}
