package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.data.QDatabaseManager;
import de.erethon.questsxl.condition.QCondition;
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

    @Override
    public void play(Quester quester) {
    }

    @Override
    public boolean conditions(Quester player) {
        for (QCondition condition : conditions) {
            if (!condition.check(player)) {
                return false;
            }
        }
        return true;
    }

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
        return this.getClass().getSimpleName();
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
