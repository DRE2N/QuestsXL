package de.erethon.questsxl.common;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.objective.QObjective;
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
            processSupplier(supplier);
        }
        plugin.getLogger().info("Processing " + QRegistries.OBJECTIVES.getEntries().size() + " registered Objectives...");
        for (Supplier<? extends QObjective> supplier : QRegistries.OBJECTIVES.getEntries().values()) {
            processSupplier(supplier);
        }
        plugin.getLogger().info("Processing " + QRegistries.CONDITIONS.getEntries().size() + " registered Conditions...");
        for (Supplier<? extends QCondition> supplier : QRegistries.CONDITIONS.getEntries().values()) {
            processSupplier(supplier);
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

    private void processSupplier(Supplier<?> supplier) {
        if (supplier == null) return;
        try {
            Object instance = supplier.get();
            if (instance == null) {
                plugin.getLogger().warning("A registered supplier returned null. Skipping.");
                return;
            }
            processClass(instance.getClass());
        } catch (Exception e) {
            plugin.getLogger().severe("Could not instantiate a class for documentation. It might be abstract or lack a default constructor. Skipping.");
            e.printStackTrace();
        }
    }

    private void processClass(Class<?> clazz) {
        QLoadableDoc loadableDoc = clazz.getAnnotation(QLoadableDoc.class);
        if (loadableDoc == null) {
            // This can happen if a registered class is not meant to be documented
            return;
        }

        plugin.getLogger().info("Processing class: " + clazz.getPackageName() + "." + clazz.getSimpleName() + " (" + loadableDoc.value() + ")");
        String category = determineCategory(clazz);
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
            entries.add(entryBuilder.toString());
        } else {
            plugin.getLogger().warning("No entries list found for category: " + category + " from class " + clazz.getName());
        }
    }


    private String determineCategory(Class<?> clazz) {
        String className = clazz.getSimpleName().toLowerCase();
        if (className.contains("action")) {
            return "Actions";
        } else if (className.contains("objective")) {
            return "Objectives";
        } else if (className.contains("condition")) {
            return "Conditions";
        }
        return "Unknown";
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