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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes({"de.erethon.questsxl.common.QLoadableDoc", "de.erethon.questsxl.common.QParamDoc"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class QDocGenerator extends AbstractProcessor {

    private final Map<String, StringBuilder> docBuilders = new HashMap<>();

    public QDocGenerator() {
        docBuilders.put("Actions", new StringBuilder("# Actions\n\n"));
        docBuilders.put("Objectives", new StringBuilder("# Objectives\n\n"));
        docBuilders.put("Conditions", new StringBuilder("# Conditions\n\n"));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(QLoadableDoc.class)) {
            QLoadableDoc loadableDoc = element.getAnnotation(QLoadableDoc.class);
            System.err.println("Processing " + element.getSimpleName());
            String category = determineCategory(element);
            StringBuilder docBuilder = docBuilders.get(category);

            if (docBuilder != null) {
                docBuilder.append("## ").append(element.getSimpleName()).append("\n");
                docBuilder.append(loadableDoc.description()).append("\n\n");

                List<? extends Element> enclosedElements = element.getEnclosedElements();
                for (Element enclosedElement : ElementFilter.fieldsIn(enclosedElements)) {
                    QParamDoc paramDoc = enclosedElement.getAnnotation(QParamDoc.class);
                    if (paramDoc != null) {
                        docBuilder.append("- **").append(enclosedElement.getSimpleName()).append("**: ")
                                .append(paramDoc.description()).append("\n");
                    }
                }
                docBuilder.append("\n");
            }
        }

        writeDocs();
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
        docBuilders.forEach((category, content) -> {
            try {
                String outputPath = "../../docs/" + category + ".md";
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