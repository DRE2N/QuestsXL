package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class QBaseAction implements QAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QPlayerCache cache = plugin.getPlayerCache();
    List<QCondition> conditions = new ArrayList<>();
    Set<QAction> runAfter = new HashSet<>();

    String id;

    @Override
    public void play(QPlayer player) {
    }

    @Override
    public void play(QEvent event) {
        event.getPlayersInRange().forEach(this::play);
    }

    @Override
    public boolean conditions(QPlayer player) {
        for (QCondition condition : conditions) {
            if (!condition.check(player)) {
                return false;
            }
        }
        return true;
    }

    public boolean conditions(QEvent event) {
        for (QCondition condition : conditions) {
            if (!condition.check(event)) {
                return false;
            }
        }
        return true;
    }

    public void onFinish(QPlayer player) {
        for (QAction action : runAfter) {
            action.play(player);
        }
    }

    public void onFinish(QEvent event) {
        for (QAction action : runAfter) {
            action.play(event);
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
    public void load(QConfig cfg) {
        id = cfg.getName();
        if (cfg.contains("runAfter")) {
            runAfter.addAll(cfg.getActions("runAfter"));
        }
        if (cfg.contains("conditions")) {
            conditions.addAll(cfg.getConditions("conditions"));
        }
    }

}
