package de.erethon.questsxl.common.script;

import de.erethon.erethonscript.ast.AssignmentNode;
import de.erethon.erethonscript.ast.BlockNode;
import de.erethon.erethonscript.ast.Statement;
import de.erethon.erethonscript.execution.CompilationContext;
import de.erethon.erethonscript.execution.ComponentCompiler;
import de.erethon.erethonscript.execution.ScriptCompiler;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QRegistry;
import de.erethon.questsxl.common.SupportsActions;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.objective.QObjective;

import java.util.HashMap;
import java.util.Map;

public class ActionCompiler implements ComponentCompiler {

    private final QRegistry<QAction> actionRegistry;

    public ActionCompiler(QRegistry<QAction> actionRegistry) {
        this.actionRegistry = actionRegistry;
    }

    public QAction compileActionBlock(BlockNode node, ScriptCompiler compiler, CompilationContext context) {
        if (node.getIdentifiers().isEmpty()) {
            throw new IllegalStateException("Compilation Error: Action block '" + node.getType() + "' requires a type identifier (e.g., 'message').");
        }
        String actionType = node.getIdentifiers().getFirst();
        QAction action = actionRegistry.get(actionType);
        if (action == null) {
            throw new IllegalStateException("Compilation Error: Unknown action type '" + actionType + "'");
        }

        context.push(action);

        Map<String, Object> configMap = new HashMap<>();
        if (node.getBody() != null) {
            for (Statement statement : node.getBody()) {
                if (statement instanceof AssignmentNode assignment) {
                    String key = assignment.getLValue().getIdentifier();
                    Object value = compiler.compileExpression(assignment.getValue(), null).evaluate(null);
                    configMap.put(key, value);
                } else if (statement instanceof BlockNode block) {
                    compiler.compileStatement(block, context);
                }
            }
        }

        if (!configMap.isEmpty()) {
            action.load(new QLineConfig(configMap));
        }

        context.pop();
        return action;
    }

    @Override
    public void compile(BlockNode node, ScriptCompiler compiler, CompilationContext context) {
        switch (node.getType()) {
            case "completeAction", "failAction", "conditionFailAction", "progressAction", "successAction", "action" -> {}
            default -> { return; }
        }

        QAction action = compileActionBlock(node, compiler, context);

        Object parent = context.peek();

        if (parent instanceof QObjective objective) {
            switch (node.getType()) {
                case "completeAction" -> objective.addCompleteAction(action);
                case "failAction" -> objective.addFailAction(action);
                case "conditionFailAction" -> objective.addConditionFailAction(action);
                case "progressAction" -> objective.addProgressAction(action);
                default -> throw new IllegalStateException("Unsupported action '" + node.getType() + "' for an Objective.");
            }
            return;
        }

        if (parent instanceof QCondition condition) {
            switch (node.getType()) {
                case "successAction" -> condition.addSuccessAction(action);
                case "failAction" -> condition.addFailAction(action);
                default -> throw new IllegalStateException("Unsupported action '" + node.getType() + "' for a Condition.");
            }
            return;
        }
        if (parent instanceof SupportsActions supportsActions) {
            supportsActions.addCompleteAction(action);
            return;
        }

        throw new IllegalStateException("Action block '" + node.getType() + "' found in an invalid context: " + parent.getClass().getSimpleName());
    }
}