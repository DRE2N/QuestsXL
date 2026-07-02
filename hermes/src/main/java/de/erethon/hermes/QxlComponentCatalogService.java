package de.erethon.hermes;

import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.QRegistry;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.script.VariableProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class QxlComponentCatalogService {

    public Map<String, Object> catalog() {
        Map<String, Object> catalog = new LinkedHashMap<>();
        List<Map<String, Object>> actions = entries(QRegistries.ACTIONS, "action");
        addSyntheticSpawnMobAction(actions);
        catalog.put("actions", actions);
        catalog.put("conditions", entries(QRegistries.CONDITIONS, "condition"));
        catalog.put("objectives", entries(QRegistries.OBJECTIVES, "objective"));
        catalog.put("variables", Map.of("builtins", builtInVariables()));
        return catalog;
    }

    private void addSyntheticSpawnMobAction(List<Map<String, Object>> actions) {
        if (actions.stream().anyMatch(entry -> "spawn_mob".equals(entry.get("id")))) {
            return;
        }
        Map<String, Map<String, Object>> paramsByName = new LinkedHashMap<>();
        paramsByName.put("mob", paramData("spawn_mob", "mob", "Aether mob id. Deprecated YAML field id is accepted and saved as mob.", "", true, "String", null));
        paramsByName.put("level", paramData("spawn_mob", "level", "Optional Aether mob level.", "", false, "int", null));
        paramsByName.put("location", paramData("spawn_mob", "location", "Optional QLocation. World is optional and defaults to Erethon at runtime.", "", false, "QLocation", null));
        paramsByName.put("amount", paramData("spawn_mob", "amount", "Number of mobs to spawn.", "", false, "int", null));
        addSyntheticParams(paramsByName, "spawn_mob", "action");
        Map<String, Object> component = new LinkedHashMap<>();
        component.put("id", "spawn_mob");
        component.put("label", "spawn_mob");
        component.put("description", "Spawn one or more Aether mobs at an optional QLocation.");
        component.put("shortExample", "spawn_mob: mob=Rossdall_Wolf; amount=4");
        component.put("longExample", List.of());
        component.put("params", new ArrayList<>(paramsByName.values()));
        component.put("providedVariables", List.of());
        actions.add(component);
        actions.sort(Comparator.comparing(entry -> String.valueOf(entry.get("id"))));
    }

    private List<Map<String, Object>> entries(QRegistry<?> registry, String category) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, ? extends Supplier<?>> entry : registry.getEntries().entrySet()) {
            Object instance;
            try {
                instance = entry.getValue().get();
            } catch (Exception ignored) {
                continue;
            }
            Class<?> type = instance.getClass();
            QLoadableDoc doc = type.getAnnotation(QLoadableDoc.class);
            Map<String, Map<String, Object>> paramsByName = new LinkedHashMap<>();
            for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    QParamDoc param = field.getAnnotation(QParamDoc.class);
                    if (param == null) {
                        continue;
                    }
                    String name = param.name().isBlank() ? field.getName() : param.name();
                    paramsByName.putIfAbsent(name, paramData(entry.getKey(), name, param.description(), param.def(), param.required(), field.getType().getSimpleName(), field));
                }
            }
            addSyntheticParams(paramsByName, entry.getKey(), category);
            List<Map<String, Object>> params = new ArrayList<>(paramsByName.values());
            Map<String, Object> component = new LinkedHashMap<>();
            component.put("id", entry.getKey());
            component.put("label", doc == null || doc.value().isBlank() ? entry.getKey() : doc.value());
            component.put("description", doc == null ? "" : doc.description());
            component.put("shortExample", doc == null ? "" : doc.shortExample());
            component.put("longExample", doc == null ? List.of() : List.of(doc.longExample()));
            component.put("params", params);
            component.put("providedVariables", providedVariables(instance));
            result.add(component);
        }
        result.sort(Comparator.comparing(e -> String.valueOf(e.get("id"))));
        return result;
    }

    private Map<String, Object> paramData(String componentId, String name, String description, String defaultValue, boolean required, String javaType, Field field) {
        Map<String, Object> paramData = new LinkedHashMap<>();
        paramData.put("name", name);
        paramData.put("description", description);
        paramData.put("defaultValue", defaultValue);
        paramData.put("required", required);
        paramData.put("javaType", javaType);
        String editorKind = editorKind(componentId, name, field, javaType);
        paramData.put("editorKind", editorKind);
        paramData.put("slotCategory", slotCategory(name));
        paramData.put("optionsSource", optionsSource(componentId, name, field, javaType));
        paramData.put("options", options(name, field));
        paramData.put("baseUnit", baseUnit(name));
        paramData.put("supportsVariables", supportsVariables(description, editorKind));
        return paramData;
    }

    private List<Map<String, Object>> builtInVariables() {
        List<Map<String, Object>> variables = new ArrayList<>();
        addVariable(variables, "quester_name", "string", "Built-in");
        addVariable(variables, "quester_x", "number", "Built-in");
        addVariable(variables, "quester_y", "number", "Built-in");
        addVariable(variables, "quester_z", "number", "Built-in");
        addVariable(variables, "quester_world", "string", "Built-in");
        addVariable(variables, "player_name", "string", "Built-in");
        addVariable(variables, "player_health", "number", "Built-in");
        addVariable(variables, "player_max_health", "number", "Built-in");
        addVariable(variables, "player_level", "number", "Built-in");
        addVariable(variables, "player_food", "number", "Built-in");
        addVariable(variables, "player_x", "number", "Built-in");
        addVariable(variables, "player_y", "number", "Built-in");
        addVariable(variables, "player_z", "number", "Built-in");
        addVariable(variables, "player_world", "string", "Built-in");
        return variables;
    }

    private List<Map<String, Object>> providedVariables(Object instance) {
        if (!(instance instanceof VariableProvider provider)) {
            return List.of();
        }
        try {
            Map<String, QVariable> provided = provider.provideVariables(null);
            if (provided == null || provided.isEmpty()) {
                return List.of();
            }
            List<Map<String, Object>> variables = new ArrayList<>();
            for (Map.Entry<String, QVariable> entry : provided.entrySet()) {
                addVariable(variables, entry.getKey(), numeric(entry.getValue()) ? "number" : "string", "Component");
            }
            variables.sort(Comparator.comparing(variable -> String.valueOf(variable.get("id"))));
            return variables;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void addVariable(List<Map<String, Object>> variables, String id, String type, String source) {
        Map<String, Object> variable = new LinkedHashMap<>();
        variable.put("id", id);
        variable.put("token", "%" + id + "%");
        variable.put("type", type);
        variable.put("source", source);
        variables.add(variable);
    }

    private boolean numeric(QVariable variable) {
        if (variable == null) {
            return false;
        }
        try {
            Double.parseDouble(variable.asString());
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean supportsVariables(String description, String editorKind) {
        String doc = description == null ? "" : description.toLowerCase();
        if (doc.contains("variable") || doc.contains("%variables%") || doc.contains("%variable%")) {
            return true;
        }
        return switch (editorKind) {
            case "string", "number", "boolean", "duration", "ticksDuration", "translatable", "minimessage" -> true;
            default -> false;
        };
    }

    private void addSyntheticParams(Map<String, Map<String, Object>> params, String componentId, String category) {
        if (isDeprecatedMobIdComponent(componentId)) {
            addSynthetic(params, componentId, "mob", "Aether mob id. Replaces deprecated id.", "", false, "String");
            addSynthetic(params, componentId, "mobs", "Aether mob ids. Replaces deprecated id lists.", "", false, "List");
        }
        if ("condition".equals(category)) {
            if (isEventIdAliasComponent(componentId)) {
                addSynthetic(params, componentId, "id", "Event id. Alias for event in older configs.", "", false, "String");
            }
            if (isQuestIdAliasComponent(componentId)) {
                addSynthetic(params, componentId, "id", "Quest id. Alias for quest in older configs.", "", false, "String");
            }
            if (isItemIdAliasComponent(componentId)) {
                addSynthetic(params, componentId, "id", "Hephaestus item id. Alias for item in older configs.", "", false, "String");
            }
            if (componentId.equalsIgnoreCase("item_in_hand")) {
                addSynthetic(params, componentId, "item", "Hephaestus item to check for.", "", false, "String");
                addSynthetic(params, componentId, "material", "Vanilla material to check for.", "", false, "String");
                addSynthetic(params, componentId, "amount", "Required amount.", "1", false, "int");
            }
        }
        switch (category) {
            case "action" -> {
                addSynthetic(params, componentId, "conditions", "Conditions required before this action can run.", "", false, "List");
                addSynthetic(params, componentId, "runAfter", "Actions to run after this action finishes.", "", false, "Set");
            }
            case "condition" -> {
                addSynthetic(params, componentId, "displayText", "Text shown for this condition.", "", false, "String");
                addSynthetic(params, componentId, "onSuccess", "Actions to run when this condition succeeds.", "", false, "Set");
                addSynthetic(params, componentId, "onFail", "Actions to run when this condition fails.", "", false, "Set");
            }
            case "objective" -> {
                addSynthetic(params, componentId, "display", "Objective display text.", "", false, "QTranslatable");
                addSynthetic(params, componentId, "hidden", "Hide this objective from the player.", "false", false, "boolean");
                addSynthetic(params, componentId, "cancel", "Cancel the triggering Bukkit event when supported.", "false", false, "boolean");
                addSynthetic(params, componentId, "conditions", "Conditions required before this objective can progress.", "", false, "Set");
                addSynthetic(params, componentId, "onConditionFail", "Actions to run when objective conditions fail.", "", false, "Set");
                addSynthetic(params, componentId, "onProgress", "Actions to run when the objective progresses.", "", false, "Set");
                addSynthetic(params, componentId, "onFail", "Actions to run when the objective fails.", "", false, "Set");
                addSynthetic(params, componentId, "onComplete", "Actions to run when the objective completes.", "", false, "Set");
                addSynthetic(params, componentId, "optional", "Allow this objective to be skipped by stage progression.", "false", false, "boolean");
                addSynthetic(params, componentId, "persistent", "Keep objective progress across reloads where supported.", "false", false, "boolean");
                addSynthetic(params, componentId, "global", "Track this objective globally instead of per player.", "false", false, "boolean");
                addSynthetic(params, componentId, "goal", "Progress goal for this objective.", "1", false, "int");
                addSynthetic(params, componentId, "scopeSuccess", "Action scope for success/completion handling.", "", false, "ActionScope");
                addSynthetic(params, componentId, "scopeProgress", "Action scope for progress actions.", "", false, "ActionScope");
                addSynthetic(params, componentId, "scopeConditionFail", "Action scope for condition-fail actions.", "", false, "ActionScope");
                addSynthetic(params, componentId, "scopeFail", "Action scope for fail actions.", "", false, "ActionScope");
                addSynthetic(params, componentId, "scopeComplete", "Action scope for completion actions.", "", false, "ActionScope");
            }
            default -> {
            }
        }
    }

    private void addSynthetic(Map<String, Map<String, Object>> params, String componentId, String name, String description, String defaultValue, boolean required, String javaType) {
        params.putIfAbsent(name, paramData(componentId, name, description, defaultValue, required, javaType, null));
    }

    private String editorKind(String componentId, String name, Field field, String javaTypeName) {
        String lowerName = name.toLowerCase();
        String javaType = javaTypeName == null ? "" : javaTypeName.toLowerCase();
        String slot = slotCategory(name);
        if (!slot.isBlank()) {
            return slot + "List";
        }
        if (lowerName.equals("display") || lowerName.equals("displaytext") || isMessageTextField(componentId, lowerName)) {
            return "translatable";
        }
        if (isMobListField(lowerName, javaType)) {
            return "mobList";
        }
        if (isMobField(componentId, lowerName)) {
            return "mob";
        }
        if (isQuestOrEventField(componentId, lowerName)) {
            return "questOrEvent";
        }
        if (lowerName.equals("id") && isEventIdAliasComponent(componentId)) {
            return "event";
        }
        if (lowerName.equals("id") && isQuestIdAliasComponent(componentId)) {
            return "quest";
        }
        if (lowerName.equals("id") && isItemIdAliasComponent(componentId)) {
            return "item";
        }
        if (lowerName.equals("quest") || lowerName.equals("questid")) {
            return "quest";
        }
        if (lowerName.equals("event") || lowerName.equals("eventid")) {
            return "event";
        }
        if (lowerName.equals("dialogue") || lowerName.equals("dialogueid")) {
            return "dialogue";
        }
        if (lowerName.equals("spell") || lowerName.equals("spellid")) {
            return "spell";
        }
        if (lowerName.equals("spells") || lowerName.equals("spellids")) {
            return "spellList";
        }
        if (lowerName.equals("trait") || lowerName.equals("traitid")) {
            return "trait";
        }
        if (lowerName.equals("traits") || lowerName.equals("traitids") || lowerName.equals("innatetraits")) {
            return "traitList";
        }
        if (lowerName.equals("effect") || lowerName.equals("effectid")) {
            return "effect";
        }
        if (lowerName.equals("effects") || lowerName.equals("effectids")) {
            return "effectList";
        }
        if (lowerName.contains("location") || lowerName.equals("target")) {
            return "location";
        }
        if (isItemListField(lowerName, javaType)) {
            return "itemList";
        }
        if (lowerName.equals("item") || lowerName.equals("block") || lowerName.endsWith("item")) {
            return "item";
        }
        if (lowerName.equals("world") || lowerName.equals("w")) {
            return "world";
        }
        if (lowerName.equals("material") || lowerName.endsWith("material")) {
            return "material";
        }
        if (lowerName.equals("vanilla") || lowerName.equals("patch")) {
            return "json";
        }
        if (isDurationField(lowerName)) {
            return isTickField(lowerName) ? "ticksDuration" : "duration";
        }
        if (javaType.equals("boolean")) {
            return "boolean";
        }
        if (javaType.equals("int") || javaType.equals("integer") || javaType.equals("long") || javaType.equals("double") || javaType.equals("float")) {
            return "number";
        }
        if ((field != null && field.getType().isEnum()) || javaType.equals("actionscope") || lowerName.equals("scope") || lowerName.startsWith("scope") || lowerName.equals("mode") || lowerName.equals("operation")) {
            return "enum";
        }
        return "string";
    }

    private String optionsSource(String componentId, String name, Field field, String javaType) {
        String kind = editorKind(componentId, name, field, javaType);
        return switch (kind) {
            case "item", "itemList" -> "hephaestus.items";
            case "mob", "mobList" -> "aether.mobs";
            case "quest" -> "qxl.quests";
            case "event" -> "qxl.events";
            case "questOrEvent" -> "qxl.quests,qxl.events";
            case "dialogue" -> "qxl.dialogues";
            case "spell", "spellList" -> "spellbook.spells";
            case "trait", "traitList" -> "spellbook.traits";
            case "effect", "effectList" -> "spellbook.effects";
            case "world" -> "bukkit.worlds";
            case "material" -> "bukkit.materials";
            default -> "";
        };
    }

    private List<String> options(String name, Field field) {
        if (name.toLowerCase().startsWith("scope")) {
            return List.of("PLAYER", "EVENT");
        }
        if (field == null || !field.getType().isEnum()) {
            return List.of();
        }
        Object[] constants = field.getType().getEnumConstants();
        List<String> result = new ArrayList<>();
        for (Object constant : constants) {
            result.add(String.valueOf(constant));
        }
        return result;
    }

    private String baseUnit(String name) {
        String lower = name.toLowerCase();
        if (isTickField(lower)) {
            return "ticks";
        }
        if (isDurationField(lower)) {
            return "seconds";
        }
        return "";
    }

    private boolean isDurationField(String lowerName) {
        return lowerName.equals("cooldown") || lowerName.equals("duration") || lowerName.equals("time") || lowerName.endsWith("time")
                || lowerName.equals("delay") || lowerName.equals("period") || isTickField(lowerName);
    }

    private boolean isTickField(String lowerName) {
        return lowerName.equals("fadein") || lowerName.equals("fadeout") || lowerName.equals("stay") || lowerName.endsWith("ticks");
    }

    private boolean isMobField(String componentId, String lowerName) {
        return lowerName.equals("mob") || lowerName.equals("mobid") || (isDeprecatedMobIdComponent(componentId) && lowerName.equals("id"));
    }

    private boolean isMobListField(String lowerName, String javaType) {
        return lowerName.equals("mobs") || lowerName.equals("mobids") || ((lowerName.endsWith("mobs") || lowerName.endsWith("mobids")) && isListType(javaType));
    }

    private boolean isDeprecatedMobIdComponent(String componentId) {
        String lower = componentId.toLowerCase();
        return lower.equals("spawn_mob") || lower.equals("interact_mob") || lower.equals("entity_interact");
    }

    private boolean isEventIdAliasComponent(String componentId) {
        String lower = componentId.toLowerCase();
        return lower.equals("event_state") || lower.equals("event_range") || lower.equals("players_in_range");
    }

    private boolean isQuestIdAliasComponent(String componentId) {
        String lower = componentId.toLowerCase();
        return lower.equals("active_quest") || lower.equals("completed_quest") || lower.equals("stage");
    }

    private boolean isItemIdAliasComponent(String componentId) {
        String lower = componentId.toLowerCase();
        return lower.equals("inventory_contains") || lower.equals("item_in_hand");
    }

    private boolean isQuestOrEventField(String componentId, String lowerName) {
        if (!lowerName.equals("id")) {
            return false;
        }
        String lowerComponent = componentId.toLowerCase();
        return lowerComponent.equals("stage") || lowerComponent.equals("objective_display_text");
    }

    private boolean isMessageTextField(String componentId, String lowerName) {
        return componentId.equalsIgnoreCase("message") && lowerName.equals("message");
    }

    private boolean isItemListField(String lowerName, String javaType) {
        return (lowerName.equals("items") || lowerName.equals("blocks")) || ((lowerName.endsWith("items") || lowerName.endsWith("blocks")) && isListType(javaType));
    }

    private boolean isListType(String javaType) {
        return javaType.equals("set") || javaType.equals("list") || javaType.endsWith("[]");
    }

    private String slotCategory(String name) {
        return switch (name) {
            case "actions", "runAfter", "onStart", "onFinish", "onUpdate", "onComplete", "onFail", "onSuccess", "onProgress", "onConditionFail", "onExpire" -> "action";
            case "conditions", "startConditions" -> "condition";
            case "objectives" -> "objective";
            default -> "";
        };
    }
}
