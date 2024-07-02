package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
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
        return null;
    }



    @Override
    public void load(QLineConfig cfg) {
    }

    @Override
    public void load(ConfigurationSection section) {
        id = section.getName();
        if (section.contains("runAfter")) {
            runAfter.addAll((Collection<? extends QAction>) QConfigLoader.load("runAfter", section, QRegistries.ACTIONS));
        }
        if (section.contains("conditions")) {
            conditions.addAll((Collection<? extends QCondition>) QConfigLoader.load("conditions", section, QRegistries.CONDITIONS));
        }
    }

}
