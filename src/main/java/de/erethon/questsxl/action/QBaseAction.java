package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.condition.ConditionManager;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

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
    public void play(Player player) {
    }

    @Override
    public boolean conditions(Player player) {
        QPlayer qPlayer = cache.get(player);
        for (QCondition condition : conditions) {
            if (!condition.check(qPlayer)) {
                return false;
            }
        }
        return true;
    }

    public void onFinish(Player player) {
        for (QAction action : runAfter) {
            action.play(player);
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
    public void load(String[] msg) {
    }

    @Override
    public void load(ConfigurationSection section) {
        id = section.getName();
        if (section.contains("runAfter")) {
            runAfter.addAll(ActionManager.loadActions(id + ": runAfter", section.getConfigurationSection("runAfter")));
        }
        if (section.contains("conditions")) {
            conditions.addAll(ConditionManager.loadConditions(id + ": Conditions", section.getConfigurationSection("conditions")));
        }
    }

}
