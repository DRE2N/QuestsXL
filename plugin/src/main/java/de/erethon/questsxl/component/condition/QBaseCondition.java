package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.component.action.QAction;
import de.erethon.questsxl.common.script.ExecutionContext;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;

import java.util.HashSet;
import java.util.Set;

public abstract class QBaseCondition implements QCondition {

    String display = "";
    private final Set<QAction> failActions = new HashSet<>();
    private final Set<QAction> successActions = new HashSet<>();

    private QComponent parent;

    /**
     * Wraps subclass check logic with ExecutionContext lifecycle and VariableProvider publishing.
     * Subclasses implement {@link #checkInternal(Quester)} instead of overriding this method.
     */
    @Override
    public final boolean check(Quester quester) {
        ExecutionContext.push(quester, this);
        try {
            return checkInternal(quester);
        } finally {
            ExecutionContext.pop();
        }
    }

    /**
     * Implement condition logic here. Call {@link #success(Quester)} or {@link #fail(Quester)} to return.
     * Variables must be published before calling fail()/success() so they are available to those actions.
     */
    protected abstract boolean checkInternal(Quester quester);

    @Override
    public boolean fail(Quester quester) {
        if (this instanceof VariableProvider provider) {
            ExecutionContext.publishVariables(provider.provideVariables(quester));
        }
        for (QAction action : failActions) {
            action.play(quester);
        }
        return false;
    }

    @Override
    public boolean success(Quester quester) {
        if (this instanceof VariableProvider provider) {
            ExecutionContext.publishVariables(provider.provideVariables(quester));
        }
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
        String displayText = cfg.getString("displayText");
        if (displayText == null || displayText.equals("none")) {
            display = null;
        } else {
            display = displayText;
        }
        if (cfg.contains("onFail")) {
            failActions.addAll(cfg.getActions(this, "onFail"));
        }
        if (cfg.contains("onSuccess")) {
            successActions.addAll(cfg.getActions(this, "onSuccess"));
        }
    }

}
