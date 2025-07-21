package de.erethon.questsxl.common.script;

import de.erethon.erethonscript.ast.AssignmentNode;
import de.erethon.erethonscript.ast.BlockNode;
import de.erethon.erethonscript.ast.Statement;
import de.erethon.erethonscript.execution.CompilationContext;
import de.erethon.erethonscript.execution.ComponentCompiler;
import de.erethon.erethonscript.execution.ScriptCompiler;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.quest.QQuest;

import java.util.HashMap;
import java.util.Map;

public class QuestCompiler implements ComponentCompiler {

    private final ActionCompiler actionCompiler; // Keep a reference to the action compiler

    public QuestCompiler(ActionCompiler actionCompiler) {
        this.actionCompiler = actionCompiler;
    }

    @Override
    public void compile(BlockNode node, ScriptCompiler compiler, CompilationContext context) {
        if (!node.getType().equals("quest")) return;

        String questId = node.getIdentifiers().getFirst();
        QQuest quest = new QQuest(questId);
        context.push(quest);

        Map<String, Object> simpleProps = new HashMap<>();
        if (node.getBody() != null) {
            for (Statement statement : node.getBody()) {
                if (statement instanceof AssignmentNode assignment) {
                    simpleProps.put(assignment.getLValue().getIdentifier(),
                            compiler.compileExpression(assignment.getValue(), null).evaluate(null));
                } else if (statement instanceof BlockNode block) {
                    switch (block.getType()) {
                        case "onStart" -> {
                            for (Statement actionStmt : block.getBody()) {
                                QAction action = actionCompiler.compileActionBlock((BlockNode) actionStmt, compiler, context);
                                quest.getStartActions().add(action);
                            }
                        }
                        case "onFinish" -> {
                            for (Statement actionStmt : block.getBody()) {
                                QAction action = actionCompiler.compileActionBlock((BlockNode) actionStmt, compiler, context);
                                quest.getFinishActions().add(action);
                            }
                        }
                        case "stage", "condition" -> compiler.compileStatement(block, context);
                        default -> throw new IllegalStateException("Unsupported block '" + block.getType() + "' inside a quest.");
                    }
                }
            }
        }
        configureQuest(quest, simpleProps);
        context.pop();
        QuestsXL.getInstance().getQuestManager().addQuest(quest);
    }

    private void configureQuest(QQuest quest, Map<String, Object> props) {
        quest.setDisplayName((String) props.getOrDefault("displayName", quest.getName()));
        quest.setDescription((String) props.getOrDefault("description", ""));
    }
}