package de.erethon.questsxl.condition;

import de.erethon.questsxl.action.ActionManager;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public abstract class QBaseCondition implements QCondition {

    String display = "";
    private final Set<QAction> failActions = new HashSet<>();
    private final Set<QAction> successActions = new HashSet<>();

    @Override
    public boolean fail(QPlayer player) {
        for (QAction action : failActions) {
            action.play(player.getPlayer());
        }
        return false;
    }

    @Override
    public boolean success(QPlayer player) {
        for (QAction action : successActions) {
            action.play(player.getPlayer());
        }
        return true;
    }

    @Override
    public boolean fail(QEvent event) {
        for (QAction action : failActions) {
            action.play(event);
        }
        return false;
    }

    @Override
    public boolean success(QEvent event) {
        for (QAction action : successActions) {
            action.play(event);
        }
        return true;
    }

    @Override
    public String getDisplayText() {
        return display;
    }

    @Override
    public void load(ConfigurationSection section) {
        if (section.getString("displayText") == null || section.getString("displayText").equals("none")) {
            display = null;
            return;
        }
        display = section.getString("displayText");
        if (section.contains("onFail")) {
            failActions.addAll(ActionManager.loadActions(section.getName() + ": onFail", section.getConfigurationSection("onFail")));
        }
        if (section.contains("onSuccess")) {
            successActions.addAll(ActionManager.loadActions(section.getName() + ": onSuccess", section.getConfigurationSection("onSuccess")));
        }
    }
    @Override
    public void load(String[] c) {

    }

}
