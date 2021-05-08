package de.erethon.questsxl.objectives;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.ActionManager;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.condition.ConditionManager;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.players.QPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.HashSet;
import java.util.Set;

public abstract class QBaseObjective implements QObjective {

    QuestsXL plugin = QuestsXL.getInstance();
    String displayText = "";
    Set<QAction> successActions = new HashSet<>();
    Set<QCondition> conditions = new HashSet<>();
    Set<QAction> conditionFailActions = new HashSet<>();
    Set<QAction> failActions = new HashSet<>();
    boolean failed = false;
    boolean optional = false;
    boolean persistent = false;

    public boolean conditions(Player player, QObjective obj) {
        QPlayer qPlayer = plugin.getPlayerCache().get(player);
        for (QCondition condition : obj.getConditions()) {
            if (!condition.check(qPlayer)) {
                return false;
            }
        }
        return true;
    }

    public void complete(Player player, QObjective obj) {
        QPlayer qPlayer = plugin.getPlayerCache().get(player);
        MessageUtil.log("Checking for completion for " + player.getName());
        for (ActiveObjective objective : qPlayer.getCurrentObjectives()) {
            if (objective.getObjective().equals(obj) && objective.getPlayer().equals(qPlayer)) {
                MessageUtil.log("Found objective");
                objective.setCompleted(true);
                objective.getStage().checkCompleted(qPlayer);
                for (QAction action : successActions) {
                    action.play(player);
                }
                if (!persistent) {
                    qPlayer.getCurrentObjectives().remove(objective);
                }
                return;
            }
        }
    }

    public void fail(Player player, QObjective obj) {
        QPlayer qPlayer = plugin.getPlayerCache().get(player);
        for (ActiveObjective objective : qPlayer.getCurrentObjectives()) {
            if (objective.getObjective().equals(obj) && objective.getPlayer().equals(qPlayer)) {
                if (!persistent) {
                    qPlayer.getCurrentObjectives().remove(objective);
                }
            }
        }
        for (QAction action : failActions) {
            action.play(player);
        }
    }

    @Override
    public boolean isFailed() {
        return failed;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    @Override
    public boolean isPersistent() {
        return persistent;
    }

    @Override
    public void check(Event event) {
    }

    @Override
    public Set<QAction> getSuccessActions() {
        return successActions;
    }

    @Override
    public Set<QAction> getFailActions() {
        return failActions;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    public Set<QCondition> getConditions() {
        return conditions;
    }

    @Override
    public Set<QAction> getConditionFailActions() {
        return conditionFailActions;
    }

    @Override
    public void load(String[] c) {

    }
    @Override
    public void load(ConfigurationSection section) {
        if (section.contains("display")) {
            displayText = section.getString("display");
        }
        if (section.contains("conditions")) {
            conditions.addAll(ConditionManager.loadConditions(section.getConfigurationSection("conditions")));
        }
        if (section.contains("onConditionFail")) {
            conditionFailActions.addAll(ActionManager.loadActions(section.getConfigurationSection("onConditionFail")));
        }
        if (section.contains("onComplete")) {
            successActions.addAll(ActionManager.loadActions(section.getConfigurationSection("onComplete")));
        }
        if (section.contains("onFail")) {
            failActions.addAll(ActionManager.loadActions(section.getConfigurationSection("onFail")));
        }
        if (section.contains("optional")) {
            optional = section.getBoolean("optional");
        }
        if (section.contains("persistent")) {
            persistent = section.getBoolean("persistent");
        }
    }
}
