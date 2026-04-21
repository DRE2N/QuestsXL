package de.erethon.questsxl.component.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.ExecutionContext;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@QLoadableDoc(
        value = "delay",
        description = "Delays the execution of a list of actions by a certain amount of time.",
        shortExample = "<no short syntax>",
        longExample = {
                "delay:",
                "  duration: 10",
                "  actions:",
                "    - 'message: message=Hello world'"
        }
)
public class DelayAction extends QBaseAction {

    private final QuestsXL plugin = QuestsXL.get();

    @QParamDoc(name = "duration", description = "The duration in seconds — supports %variables%", def="0", required = true)
    private String rawDelay;
    @QParamDoc(name = "actions", description = "The list of actions to execute after the delay", required = true)
    private Set<QAction> actions;

    @Override
    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        // Resolve before scheduling — ExecutionContext won't be available on later ticks
        ExecutionContext ctx = ExecutionContext.current();
        final long delay = ctx != null ? ctx.resolveLong(rawDelay) : parseLong(rawDelay);
        List<QAction> tmp = new ArrayList<>(actions);
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (QAction action : tmp) {
                    action.play(quester);
                }
                onFinish(quester);
            }
        };
        runnable.runTaskLater(plugin, delay * 20);
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        actions = cfg.getActions(this, "actions");
        rawDelay = cfg.getString("duration", "0");
        if (actions.isEmpty()) {
            throw new RuntimeException("Action list is empty.");
        }
    }
}
