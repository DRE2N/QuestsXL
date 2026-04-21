package de.erethon.questsxl.common.doc;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.component.action.QAction;
import de.erethon.questsxl.component.condition.QCondition;
import de.erethon.questsxl.component.objective.QObjective;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class RuntimeDocGenerator {

    private final Map<String, StringBuilder> docBuilders = new HashMap<>();
    private final Map<String, List<String>> entriesMap = new HashMap<>();
    private final Path outputDir;
    private final QuestsXL plugin;

    public RuntimeDocGenerator(QuestsXL plugin, Path outputDir) {
        this.plugin = plugin;
        this.outputDir = outputDir;
        docBuilders.put("Actions", new StringBuilder());
        docBuilders.put("Objectives", new StringBuilder());
        docBuilders.put("Conditions", new StringBuilder());
        entriesMap.put("Actions", new ArrayList<>());
        entriesMap.put("Objectives", new ArrayList<>());
        entriesMap.put("Conditions", new ArrayList<>());
    }

    public void generate() {
        plugin.getLogger().info("Starting documentation generation from registered entries...");

        plugin.getLogger().info("Processing " + QRegistries.ACTIONS.getEntries().size() + " registered Actions...");
        for (Supplier<? extends QAction> supplier : QRegistries.ACTIONS.getEntries().values()) {
            processSupplier(supplier, "Actions");
        }
        plugin.getLogger().info("Processing " + QRegistries.OBJECTIVES.getEntries().size() + " registered Objectives...");
        for (Supplier<? extends QObjective> supplier : QRegistries.OBJECTIVES.getEntries().values()) {
            processSupplier(supplier, "Objectives");
        }
        plugin.getLogger().info("Processing " + QRegistries.CONDITIONS.getEntries().size() + " registered Conditions...");
        for (Supplier<? extends QCondition> supplier : QRegistries.CONDITIONS.getEntries().values()) {
            processSupplier(supplier, "Conditions");
        }

        entriesMap.forEach((category, entries) -> {
            entries.sort(Comparator.naturalOrder());
            StringBuilder docBuilder = docBuilders.get(category);
            if (docBuilder != null) {
                for (String entry : entries) {
                    docBuilder.append(entry);
                }
            }
        });

        writeDocs();
        plugin.getLogger().info("Finished documentation generation. Files written to: " + outputDir.toAbsolutePath());
    }

    private void processSupplier(Supplier<?> supplier, String category) {
        if (supplier == null) return;
        try {
            Object instance = supplier.get();
            if (instance == null) {
                plugin.getLogger().warning("A registered supplier returned null. Skipping.");
                return;
            }
            processClass(instance, category);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not instantiate a class for documentation. It might be abstract or lack a default constructor. Skipping.");
            e.printStackTrace();
        }
    }

    private void processClass(Object instance, String category) {
        Class<?> clazz = instance.getClass();
        QLoadableDoc loadableDoc = clazz.getAnnotation(QLoadableDoc.class);
        if (loadableDoc == null) {
            // This can happen if a registered class is not meant to be documented
            return;
        }

        plugin.getLogger().info("Processing class: " + clazz.getPackageName() + "." + clazz.getSimpleName() + " (" + loadableDoc.value() + ")");
        List<String> entries = entriesMap.get(category);

        if (entries != null) {
            StringBuilder entryBuilder = new StringBuilder();
            entryBuilder.append("## ").append(loadableDoc.value()).append("\n");
            entryBuilder.append(loadableDoc.description()).append("\n\n");
            entryBuilder.append("#### Parameters:\n\n");
            entryBuilder.append("| Parameter | Description | Default | Required |\n");
            entryBuilder.append("|-----------|-------------|---------|----------|\n");

            List<String> paramEntries = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                QParamDoc paramDoc = field.getAnnotation(QParamDoc.class);
                if (paramDoc != null) {
                    String paramName = paramDoc.name().isEmpty() ? field.getName() : paramDoc.name();
                    paramEntries.add("| `" + paramName + "` | " + paramDoc.description() + " | " + paramDoc.def() + " | " + paramDoc.required() + " |\n");
                }
            }

            paramEntries.sort(Comparator.naturalOrder());

            for (String paramEntry : paramEntries) {
                entryBuilder.append(paramEntry);
            }

            entryBuilder.append("\n```yaml\n").append(loadableDoc.shortExample()).append("\n```\n\n");
            entryBuilder.append("```yaml\n").append(String.join("\n", loadableDoc.longExample())).append("\n```\n\n");

            // Variables table — only for VariableProvider implementations
            String variablesTable = buildVariablesTable(instance);
            if (variablesTable != null) {
                entryBuilder.append(variablesTable);
            }

            entries.add(entryBuilder.toString());
        } else {
            plugin.getLogger().warning("No entries list found for category: " + category + " from class " + clazz.getName());
        }
    }


    /**
     * If the instance implements {@link VariableProvider}, calls {@code provideVariables(null)}
     * to obtain the variable names and their default values, then returns a markdown table.
     * Returns {@code null} if the class is not a {@link VariableProvider} or the call fails.
     */
    private String buildVariablesTable(Object instance) {
        if (!(instance instanceof VariableProvider provider)) {
            return null;
        }
        Map<String, QVariable> vars;
        try {
            // null quester is safe — all provideVariables() implementations only read
            // pre-stored lastX fields and never dereference the quester argument directly.
            vars = provider.provideVariables(null);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not call provideVariables() on "
                    + instance.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
        if (vars == null || vars.isEmpty()) {
            return null;
        }

        StringBuilder table = new StringBuilder();
        table.append("#### Provided variables:\n\n");
        table.append("| Variable | Type |\n");
        table.append("|----------|------|\n");

        vars.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String type = isNumeric(entry.getValue()) ? "number" : "string";
                    table.append("| `%").append(entry.getKey()).append("%` | ").append(type).append(" |\n");
                });

        table.append("\n");
        return table.toString();
    }

    /** Returns true if the variable's default value looks like a number. */
    private boolean isNumeric(QVariable var) {
        if (var == null) return false;
        try {
            Double.parseDouble(var.asString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void writeDocs() {
        docBuilders.forEach((category, content) -> {
            try {
                Path outputPath = outputDir.resolve(category.toLowerCase() + ".md");
                if (!Files.exists(outputDir)) {
                    Files.createDirectories(outputDir);
                }
                try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
                    writer.write(getHeader(category));
                    writer.write(content.toString());
                    plugin.getLogger().info("Wrote " + category + ".md (Path: " + outputPath.toAbsolutePath() + ")");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private String getHeader(String category) {
        int sidebarPosition = switch (category) {
            case "Actions" -> 3;
            case "Conditions" -> 4;
            case "Objectives" -> 5;
            default -> 0;
        };
        return "---\n" +
                "title: " + category + "\n" +
                "sidebar_position: " + sidebarPosition + "\n" +
                "---\n\n";
    }
}