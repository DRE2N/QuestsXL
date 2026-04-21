package de.erethon.questsxl.component.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.ExecutionContext;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
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

    private transient final QuestsXL plugin = QuestsXL.get();

    @QParamDoc(name = "delay", description = "The delay between each repetition in seconds — supports %variables%", def="0")
    String rawDelay;
    @QParamDoc(name = "repetitions", description = "The amount of repetitions — supports %variables%", def = "1")
    String rawRepetitions;
    @QParamDoc(name = "actions", description = "The list of actions to repeat", required = true)
    Set<QAction> actions;

    int current = 0;

    BukkitRunnable task;

    @Override
    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        // Resolve before scheduling — ExecutionContext won't be available on later ticks
        ExecutionContext ctx = ExecutionContext.current();
        final long delay = ctx != null ? ctx.resolveLong(rawDelay) : parseLong(rawDelay);
        final int repetitions = ctx != null ? ctx.resolveInt(rawRepetitions) : parseInt(rawRepetitions);
        cancel();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (current >= repetitions) {
                    onFinish(quester);
                    cancel();
                    return;
                }
                current++;
                for (QAction action : actions) {
                    try {
                        action.play(quester);
                    } catch (Exception e) {
                        FriendlyError error = new FriendlyError(id,"Failed to execute repeat actions", e.getMessage(), "Player: " + quester.getName()).addStacktrace(e.getStackTrace());
                        plugin.addRuntimeError(error);
                    }
                }
            }
        };
        task.runTaskTimer(plugin, delay * 20, delay * 20);
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0; }
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 1; }
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
        rawDelay = cfg.getString("delay", "0");
        rawRepetitions = cfg.getString("repetitions", "1");
    }
}
