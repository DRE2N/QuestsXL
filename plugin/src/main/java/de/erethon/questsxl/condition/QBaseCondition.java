package de.erethon.questsxl.condition;

import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.Quester;

import java.util.HashSet;
import java.util.Set;

public abstract class QBaseCondition implements QCondition {

    String display = "";
    private final Set<QAction> failActions = new HashSet<>();
    private final Set<QAction> successActions = new HashSet<>();

    private QComponent parent;

    @Override
    public boolean fail(Quester quester) {
        for (QAction action : failActions) {
            action.play(quester);
        }
        return false;
    }

    @Override
    public boolean success(Quester quester) {
        for (QAction action : successActions) {
            action.play(quester);
        }
        return true;
    }

    @Override
    public String getDisplayText() {
        return display;
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
        if (cfg.getString("displayText") == null || cfg.getString("displayText").equals("none")) {
            display = null;
            return;
        }
        display = cfg.getString("displayText");
        if (cfg.contains("onFail")) {
            failActions.addAll(cfg.getActions(this, "onFail"));
        }
        if (cfg.contains("onSuccess")) {
            successActions.addAll(cfg.getActions(this, "onSuccess"));
        }
    }

}
