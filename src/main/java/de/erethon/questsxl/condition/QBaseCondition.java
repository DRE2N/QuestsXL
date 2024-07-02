package de.erethon.questsxl.condition;

import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class QBaseCondition implements QCondition {

    String display = "";
    private final Set<QAction> failActions = new HashSet<>();
    private final Set<QAction> successActions = new HashSet<>();

    @Override
    public boolean fail(QPlayer player) {
        for (QAction action : failActions) {
            action.play(player);
        }
        return false;
    }

    @Override
    public boolean success(QPlayer player) {
        for (QAction action : successActions) {
            action.play(player);
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
            failActions.addAll((Collection<? extends QAction>) QConfigLoader.load("onFail", section, QRegistries.ACTIONS));
        }
        if (section.contains("onSuccess")) {
            successActions.addAll((Collection<? extends QAction>) QConfigLoader.load("onSuccess", section, QRegistries.ACTIONS));
        }
    }
    @Override
    public void load(QLineConfig section) {

    }

}
