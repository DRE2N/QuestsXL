package de.erethon.questsxl.common.script;

import de.erethon.erethonscript.ast.AssignmentNode;
import de.erethon.erethonscript.ast.BlockNode;
import de.erethon.erethonscript.ast.Statement;
import de.erethon.erethonscript.execution.CompilationContext;
import de.erethon.erethonscript.execution.ComponentCompiler;
import de.erethon.erethonscript.execution.ScriptCompiler;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QRegistry;

import java.util.HashMap;
import java.util.Map;

public abstract class QComponentCompiler<T extends QComponent> implements ComponentCompiler {

    protected abstract String getSupportedBlockType();
    protected abstract T createInstance(String typeIdentifier, QRegistry<T> registry);
    protected abstract void attachToParent(T instance, CompilationContext context);
    protected abstract void configureInstance(T instance, Map<String, Object> config);
    protected abstract void processNestedBlock(T instance, BlockNode block, ScriptCompiler compiler, CompilationContext context);
    protected abstract QRegistry<T> getRegistry();

    @Override
    public void compile(BlockNode node, ScriptCompiler compiler, CompilationContext context) {
        if (!node.getType().equals(getSupportedBlockType())) {
            return;
        }

        if (node.getIdentifiers().isEmpty()) {
            throw new IllegalStateException("Compilation Error: '" + getSupportedBlockType() + "' block requires a type identifier.");
        }
        String componentType = node.getIdentifiers().getFirst();
        T instance = createInstance(componentType, getRegistry());

        attachToParent(instance, context);

        context.push(instance);

        Map<String, Object> configMap = new HashMap<>();
        if (node.getBody() != null) {
            for (Statement statement : node.getBody()) {
                if (statement instanceof AssignmentNode assignment) {
                    String key = assignment.getLValue().getIdentifier();
                    Object value = compiler.compileExpression(assignment.getValue(), null).evaluate(null);
                    configMap.put(key, value);
                } else if (statement instanceof BlockNode block) {
                    processNestedBlock(instance, block, compiler, context);
                } else {
                    throw new IllegalStateException("Unsupported statement type inside '" + getSupportedBlockType() + "' block: " + statement.getClass().getSimpleName());
                }
            }
        }

        configureInstance(instance, configMap);
        context.pop();
    }
}