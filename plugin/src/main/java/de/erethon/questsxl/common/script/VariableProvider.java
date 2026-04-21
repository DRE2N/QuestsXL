package de.erethon.questsxl.common.script;

import de.erethon.questsxl.common.Quester;

import java.util.Map;

/**
 * Implemented by QComponents that expose runtime variables to their descendants.
 * Called once per execution frame, results are cached in the current {@link ExecutionContext}.
 *
 * Example: GroupSizeCondition implements this and returns {"group_size": <actual size>}
 * after its check() runs, so child actions can use %group_size%.
 */
public interface VariableProvider {

    /**
     * Returns a map of variable names to values that this component exposes.
     * This is called during execution (play/check) and the results are merged
     * into the current {@link ExecutionContext} cache.
     *
     * @param quester the quester this is being executed for
     * @return a map of variable name -> QVariable, never null
     */
    Map<String, QVariable> provideVariables(Quester quester);
}

