package de.erethon.questsxl.objective;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.ActionManager;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.condition.ConditionManager;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.HashSet;
import java.util.Set;

public abstract class QBaseObjective implements QObjective {

    QuestsXL plugin = QuestsXL.getInstance();
    String displayText;
    Set<QAction> successActions = new HashSet<>();
    Set<QCondition> conditions = new HashSet<>();
    Set<QAction> conditionFailActions = new HashSet<>();
    Set<QAction> failActions = new HashSet<>();
    boolean failed = false;
    boolean optional = false;
    boolean persistent = false;
    boolean isGlobal = false;

    public boolean conditions(Player player) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        QPlayer qPlayer = plugin.getPlayerCache().getByPlayer(player);
        for (QCondition condition : conditions) {
            if (!condition.check(qPlayer)) {
                condFail(player);
                return false;
            }
        }
        return true;
    }

    public void complete(Player player, QObjective obj) {
        QPlayer qPlayer = plugin.getPlayerCache().getByPlayer(player);
        MessageUtil.log("Checking for completion for " + player.getName());
        for (ActiveObjective objective : qPlayer.getCurrentObjectives()) {
            if (objective.getObjective().equals(obj) && objective.getPlayer().equals(qPlayer)) {
                MessageUtil.log("Found objective");
                objective.setCompleted(true);
                if (objective.getStage() != null) {
                    objective.getStage().checkCompleted(qPlayer);
                }
                if (successActions != null && !successActions.isEmpty()) {
                    MessageUtil.log("Playing actions...");
                    for (QAction action : successActions) {
                        action.play(player);
                    }
                }
                if (!persistent) {
                    qPlayer.getCurrentObjectives().remove(objective);
                }
                return;
            }
        }
    }

    public boolean condFail(Player pl) {
        for (QAction action : conditionFailActions) {
            action.play(pl);
        }
        return false;
    }

    public void fail(Player player, QObjective obj) {
        QPlayer qPlayer = plugin.getPlayerCache().getByPlayer(player);
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
    public boolean isGlobal() {
        return isGlobal;
    }

    @Override
    public void setGlobal(boolean global) {
        isGlobal = global;
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
            conditions.addAll(ConditionManager.loadConditions(section.getName() + ": conditions", section.getConfigurationSection("conditions")));
        }
        if (section.contains("onConditionFail")) {
            conditionFailActions.addAll(ActionManager.loadActions(section.getName() + ": onConditionFail", section.getConfigurationSection("onConditionFail")));
        }
        if (section.contains("onComplete")) {
            successActions.addAll(ActionManager.loadActions(section.getName() + ": onComplete", section.getConfigurationSection("onComplete")));
        }
        if (section.contains("onFail")) {
            failActions.addAll(ActionManager.loadActions(section.getName() + ": onFail", section.getConfigurationSection("onFail")));
        }
        if (section.contains("optional")) {
            optional = section.getBoolean("optional");
        }
        if (section.contains("persistent")) {
            persistent = section.getBoolean("persistent");
        }
    }

    @Override
    public void onStart(QPlayer player) {

    }
}
