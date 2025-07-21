package de.erethon.questsxl.common.script;

import de.erethon.erethonscript.ast.BlockNode;
import de.erethon.erethonscript.execution.CompilationContext;
import de.erethon.erethonscript.execution.ScriptCompiler;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QRegistry;
import de.erethon.questsxl.common.SupportsObjectives;
import de.erethon.questsxl.common.script.ActionCompiler;
import de.erethon.questsxl.common.script.QComponentCompiler;
import de.erethon.questsxl.objective.QObjective;

import java.util.Map;

public class ObjectiveCompiler extends QComponentCompiler<QObjective> {

    private final QRegistry<QObjective> objectiveRegistry;
    private final ActionCompiler actionCompiler; // Needs a reference

    public ObjectiveCompiler(QRegistry<QObjective> objectiveRegistry, ActionCompiler actionCompiler) {
        this.objectiveRegistry = objectiveRegistry;
        this.actionCompiler = actionCompiler;
    }

    @Override
    protected String getSupportedBlockType() { return "objective"; }

    @Override
    protected QRegistry<QObjective> getRegistry() { return objectiveRegistry; }

    @Override
    protected QObjective createInstance(String typeIdentifier, QRegistry<QObjective> registry) {
        QObjective<?> objective = registry.get(typeIdentifier);
        if (objective == null) throw new IllegalStateException("Unknown objective type '" + typeIdentifier + "'");
        return objective;
    }

    @Override
    protected void attachToParent(QObjective instance, CompilationContext context) {
        context.peek(SupportsObjectives.class).addObjective(instance);
    }

    @Override
    protected void configureInstance(QObjective instance, Map<String, Object> config) {
        if (!config.isEmpty()) instance.load(new QLineConfig(config));
    }

    @Override
    protected void processNestedBlock(QObjective instance, BlockNode block, ScriptCompiler compiler, CompilationContext context) {
        switch (block.getType()) {
            case "completeAction" -> instance.getCompleteActions().add(actionCompiler.compileActionBlock(block, compiler, context));
            case "failAction" -> instance.getFailActions().add(actionCompiler.compileActionBlock(block, compiler, context));
            case "conditionFailAction" -> instance.getConditionFailActions().add(actionCompiler.compileActionBlock(block, compiler, context));
            case "progressAction" -> instance.getProgressActions().add(actionCompiler.compileActionBlock(block, compiler, context));
            case "condition" -> compiler.compileStatement(block, context);
            default -> throw new IllegalStateException("Unsupported block '" + block.getType() + "' inside an objective.");
        }
    }
}