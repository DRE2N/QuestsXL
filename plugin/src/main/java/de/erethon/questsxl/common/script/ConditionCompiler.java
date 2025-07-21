package de.erethon.questsxl.common.script;

import de.erethon.erethonscript.ast.BlockNode;
import de.erethon.erethonscript.execution.CompilationContext;
import de.erethon.erethonscript.execution.ScriptCompiler;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QRegistry;
import de.erethon.questsxl.common.SupportsConditions;
import de.erethon.questsxl.condition.QCondition;

import java.util.Map;

public class ConditionCompiler extends QComponentCompiler<QCondition> {

    private final QRegistry<QCondition> conditionRegistry;

    public ConditionCompiler(QRegistry<QCondition> conditionRegistry) {
        this.conditionRegistry = conditionRegistry;
    }

    @Override
    protected String getSupportedBlockType() {
        return "condition";
    }

    @Override
    protected QCondition createInstance(String typeIdentifier, QRegistry<QCondition> registry) {
        QCondition condition = registry.get(typeIdentifier);
        if (condition == null) {
            throw new IllegalStateException("Compilation Error: Unknown condition type '" + typeIdentifier + "'");
        }
        return condition;
    }

    @Override
    protected void attachToParent(QCondition instance, CompilationContext context) {
        SupportsConditions parent = context.peek(SupportsConditions.class);
        parent.addCondition(instance);
    }

    @Override
    protected void configureInstance(QCondition instance, Map<String, Object> config) {
        if (!config.isEmpty()) {
            instance.load(new QLineConfig(config));
        }
    }

    @Override
    protected void processNestedBlock(QCondition instance, BlockNode block, ScriptCompiler compiler, CompilationContext context) {
        // Todo
    }

    @Override
    protected QRegistry<QCondition> getRegistry() {
        return conditionRegistry;
    }
}