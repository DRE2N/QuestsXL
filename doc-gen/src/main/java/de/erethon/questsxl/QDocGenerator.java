package de.erethon.questsxl;

import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes({"de.erethon.questsxl.common.QLoadableDoc", "de.erethon.questsxl.common.QParamDoc"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class QDocGenerator extends AbstractProcessor {

    private final Map<String, StringBuilder> docBuilders = new HashMap<>();
    private final Map<String, List<String>> entriesMap = new HashMap<>();
    private boolean processed = false; // Gradle seems to call it twice, but I don't care enough to fix it

    public QDocGenerator() {
        docBuilders.put("Actions", new StringBuilder("# Actions\n\n"));
        docBuilders.put("Objectives", new StringBuilder("# Objectives\n\n"));
        docBuilders.put("Conditions", new StringBuilder("# Conditions\n\n"));
        entriesMap.put("Actions", new ArrayList<>());
        entriesMap.put("Objectives", new ArrayList<>());
        entriesMap.put("Conditions", new ArrayList<>());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (processed) {
            return false;
        }
        processed = true;

        for (Element element : roundEnvironment.getElementsAnnotatedWith(QLoadableDoc.class)) {
            QLoadableDoc loadableDoc = element.getAnnotation(QLoadableDoc.class);
            System.err.println("Processing " + element.getSimpleName());
            String category = determineCategory(element);
            List<String> entries = entriesMap.get(category);

            if (entries != null) {
                StringBuilder entryBuilder = new StringBuilder();
                entryBuilder.append("## ").append(loadableDoc.value()).append("\n");
                entryBuilder.append(loadableDoc.description()).append("\n\n");
                entryBuilder.append("#### Parameters:\n\n");
                entryBuilder.append("| Parameter | Description | Default | Required |\n");
                entryBuilder.append("|-----------|-------------|---------|----------|\n");

                List<String> paramEntries = new ArrayList<>();
                List<? extends Element> enclosedElements = element.getEnclosedElements();
                for (Element enclosedElement : ElementFilter.fieldsIn(enclosedElements)) {
                    QParamDoc paramDoc = enclosedElement.getAnnotation(QParamDoc.class);
                    if (paramDoc != null) {
                        paramEntries.add("| `" + paramDoc.name() + "` | " + paramDoc.description() + " | " + paramDoc.def() + " | " + paramDoc.required() + " |\n");
                    }
                }

                // Sort parameters alphabetically
                paramEntries.sort(Comparator.naturalOrder());

                // Append sorted parameters to the entryBuilder
                for (String paramEntry : paramEntries) {
                    entryBuilder.append(paramEntry);
                }

                entryBuilder.append("\n```yaml\n").append(loadableDoc.shortExample()).append("\n```\n\n");
                entryBuilder.append("```yaml\n").append(String.join("\n", loadableDoc.longExample())).append("\n```\n\n");
                entries.add(entryBuilder.toString());
            } else {
                System.err.println("No entries list found for category: " + category + " - Make sure all classes include Action/Objective/Condition in their name.");
            }
        }

        // Sort and append entries to the docBuilder
        entriesMap.forEach((category, entries) -> {
            entries.sort(Comparator.naturalOrder());
            StringBuilder docBuilder = docBuilders.get(category);
            if (docBuilder != null) {
                for (String entry : entries) {
                    docBuilder.append(entry);
                }
            }
        });

        // Write docs after processing all elements
        writeDocs();
        System.err.println("Finished processing.");
        return true;
    }

    private String determineCategory(Element element) {
        String className = element.getSimpleName().toString().toLowerCase();
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
        String outputDirPath = processingEnv.getOptions().get("docOutputDir");
        if (outputDirPath == null) {
            outputDirPath = "docs";
        }
        java.nio.file.Path outputDir = Paths.get(outputDirPath);

        docBuilders.forEach((category, content) -> {
            try {
                String outputPath = outputDir.resolve(category + ".md").toString();
                if (!Files.exists(outputDir)) {
                    Files.createDirectories(outputDir);
                }
                try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputPath)))) {
                    writer.write(content.toString());
                    System.err.println("Wrote " + category + ".md (Path: " + Paths.get(outputPath).toAbsolutePath() + ")");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}