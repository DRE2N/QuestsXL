package de.erethon.questsxl.component.objective;

import de.erethon.questsxl.component.action.QAction;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Set;

@QLoadableDoc(
        value = "timer",
        description = "Special objective. Starts a countdown timer. When all other current objectives of this Quest/Event are completed before the timer runs out, the onComplete actions are executed and the objective is marked as complete. If the timer runs out before all other objectives are completed, the onExpire actions are executed.",
        shortExample = "<no short format>",
        longExample = {
                "timer:",
                "  time: 120 # Time in seconds",
                "  onExpire:",
                "    - message: message=You ran out of time!",
                "  onComplete:",
                "    - message: message=You completed the objectives in time!"
        }
)
public class TimerObjective extends QBaseObjective implements VariableProvider {

    @QParamDoc(name = "time", description = "The time in seconds for the countdown timer.", required = true)
    private int timerSeconds;
    @QParamDoc(name = "onExpire", description = "Actions to execute when the timer expires.")
    private Set<QAction> expireActions;
    @QParamDoc(name = "onComplete", description = "Actions to execute when all other objectives are completed before the timer expires.")
    private Set<QAction> completeActions;

    private BukkitRunnable timerTask;
    private QTranslatable message;
    private int secondsLeft = 0;

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("<time>");
    }

    @Override
    public QTranslatable getDisplayText(Player player) {
        return message;
    }

    @Override
    public Class getEventType() {
        return null;
    }

    @Override
    public void check(ActiveObjective activeObjective, Event event) {
        Set<ActiveObjective> activeObjectives = activeObjective.getHolder().getCurrentObjectives();
        boolean allOthersCompleted = true;
        for (ActiveObjective ao : activeObjectives) {
            if (ao != activeObjective && (!ao.isCompleted() && (!ao.getObjective().isPersistent() || !ao.getObjective().isOptional()))) {
                allOthersCompleted = false;
                break;
            }
        }
        if (allOthersCompleted) {
            if (timerTask != null) {
                timerTask.cancel();
                for (QAction action : completeActions) {
                    if (activeObjective.getHolder() instanceof QEvent qEvent) {
                        action.play(qEvent);
                    }
                    if (activeObjective.getHolder() instanceof QPlayer qPlayer) {
                        action.play(qPlayer);
                    }
                }
            }
            checkCompletion(activeObjective, this);
        }
    }

    /** Exposes %seconds_left% to child actions. */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("seconds_left", new QVariable(secondsLeft));
    }

    @Override
    public void onStart(ObjectiveHolder holder) {
        super.onStart(holder);
        secondsLeft = timerSeconds;
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    for (QAction action : expireActions) {
                        if (holder instanceof QEvent qEvent) {
                            action.play(qEvent);
                        }
                        if (holder instanceof QPlayer qPlayer) {
                            action.play(qPlayer);
                        }
                    }
                    timerTask.cancel();
                } else {
                    secondsLeft--;
                }
                // Format time message
                int minutes = secondsLeft / 60;
                int seconds = secondsLeft % 60;
                message = QTranslatable.fromString("en=Time left: " + String.format("%02d:%02d", minutes, seconds) +
                        "; de=Verbleibende Zeit: " + String.format("%02d:%02d", minutes, seconds));
            }
        };
        timerTask.runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        timerSeconds = cfg.getInt("time", 60);
        expireActions = cfg.getActions(this,  "onExpire");
        completeActions = cfg.getActions(this, "onComplete");
    }
}
