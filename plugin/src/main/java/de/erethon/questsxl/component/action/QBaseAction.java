package de.erethon.questsxl.component.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.ExecutionContext;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.common.data.QDatabaseManager;
import de.erethon.questsxl.component.condition.QCondition;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public abstract class QBaseAction implements QAction {

    QuestsXL plugin = QuestsXL.get();
    QDatabaseManager databaseManager = plugin.getDatabaseManager();
    List<QCondition> conditions = new ArrayList<>();
    Set<QAction> runAfter = new HashSet<>();

    private QComponent parent;

    public String id;

    /**
     * Final entry point — pushes an {@link ExecutionContext} for this frame, publishes any
     * variables from {@link VariableProvider} implementations, delegates to {@link #playInternal},
     * then pops the context. All subclasses override {@code playInternal} instead of this method.
     */
    @Override
    public final void play(Quester quester) {
        ExecutionContext.push(quester, this);
        try {
            if (this instanceof VariableProvider provider) {
                ExecutionContext.publishVariables(provider.provideVariables(quester));
            }
            playInternal(quester);
        } finally {
            ExecutionContext.pop();
        }
    }

    /**
     * Implement action logic here. An {@link ExecutionContext} is always active when this is called.
     * Call {@link #onFinish(Quester)} at the end of your implementation to run any chained actions.
     */
    public abstract void playInternal(Quester quester);

    @Override
    public boolean conditions(Quester player) {
        try (var frame = ExecutionContext.frame(player, this)) {
            for (QCondition condition : conditions) {
                if (!condition.check(player)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Runs all {@code runAfter} actions. An {@link ExecutionContext} is active (inherited from
     * the enclosing {@link #play} call), so variables are available to chained actions.
     */
    @Override
    public void onFinish(Quester quester) {
        for (QAction action : runAfter) {
            action.play(quester);
        }
    }

    protected void execute(Quester quester, Consumer<QPlayer> action) {
        if (quester instanceof QPlayer player) {
            action.accept(player);
        } else if (quester instanceof QEvent event) {
            for (QPlayer player : event.getPlayersInRange()) {
                action.accept(player);
            }
        }
    }

    @Override
    public void delayedEnd(int seconds) {
    }

    @Override
    public void cancel() {
    }

    @Override
    public Material getIcon() {
        return Material.BEDROCK;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public QComponent getParent() {
        return parent;
    }

    @Override
    public void setParent(QComponent parent) {
        this.parent = parent;
    }

    @Override
    public String id() {
        return "in: " + findTopParent().id() + " - " + this.getClass().getSimpleName();
    }

    @Override
    public void load(QConfig cfg) {
        id = cfg.getName();
        if (cfg.contains("runAfter")) {
            runAfter.addAll(cfg.getActions(this, "runAfter"));
        }
        if (cfg.contains("conditions")) {
            conditions.addAll(cfg.getConditions(this, "conditions"));
        }
    }
}
