package de.erethon.questsxl.common.script;

import de.erethon.erethonscript.ast.AssignmentNode;
import de.erethon.erethonscript.ast.BlockNode;
import de.erethon.erethonscript.ast.Statement;
import de.erethon.erethonscript.execution.CompilationContext;
import de.erethon.erethonscript.execution.ComponentCompiler;
import de.erethon.erethonscript.execution.ScriptCompiler;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.Completable;
import de.erethon.questsxl.common.QStage;

import java.util.HashMap;
import java.util.Map;

public class StageCompiler implements ComponentCompiler {

    private final ActionCompiler actionCompiler;

    public StageCompiler(ActionCompiler actionCompiler) {
        this.actionCompiler = actionCompiler;
    }

    @Override
    public void compile(BlockNode node, ScriptCompiler compiler, CompilationContext context) {
        if (!node.getType().equals("stage")) {
            return;
        }

        if (node.getIdentifiers().isEmpty()) {
            throw new IllegalStateException("Compilation Error: 'stage' block requires a numeric ID.");
        }
        int stageId;
        try {
            stageId = Integer.parseInt(node.getIdentifiers().getFirst());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Compilation Error: 'stage' ID must be a valid integer, but found '" + node.getIdentifiers().getFirst() + "'");
        }

        Completable parent = context.peek(Completable.class);
        QStage stage = new QStage(parent, stageId);
        parent.getStages().add(stage);

        context.push(stage);

        Map<String, Object> simpleProps = new HashMap<>();
        if (node.getBody() != null) {
            for (Statement statement : node.getBody()) {
                if (statement instanceof AssignmentNode assignment) {
                    String key = assignment.getLValue().getIdentifier();
                    Object value = compiler.compileExpression(assignment.getValue(), null).evaluate(null);
                    simpleProps.put(key, value);
                } else if (statement instanceof BlockNode block) {
                    switch (block.getType()) {
                        case "onStart" -> {
                            if (block.getBody() != null) {
                                for (Statement actionStmt : block.getBody()) {
                                    QAction action = actionCompiler.compileActionBlock((BlockNode) actionStmt, compiler, context);
                                    stage.getStartActions().add(action);
                                }
                            }
                        }
                        case "onFinish" -> {
                            if (block.getBody() != null) {
                                for (Statement actionStmt : block.getBody()) {
                                    QAction action = actionCompiler.compileActionBlock((BlockNode) actionStmt, compiler, context);
                                    stage.getCompleteActions().add(action);
                                }
                            }
                        }
                        case "objective", "condition" -> {
                            compiler.compileStatement(block, context);
                        }
                        default -> {
                            throw new IllegalStateException("Unsupported block type '" + block.getType() + "' inside a stage.");
                        }
                    }
                }
            }
        }
        context.pop();
    }
}