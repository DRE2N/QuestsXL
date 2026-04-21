package de.erethon.questsxl.common.script;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QRegistry;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QConfigLoader {

    public static Set<? extends QComponent> load(QComponent component, String id, ConfigurationSection parentSection, QRegistry<?> registry, String source) {
        String resolvedSource = source == null || source.isBlank() ? "unknown" : source;
        if (parentSection.isConfigurationSection(id)) {
            ConfigurationSection section = parentSection.getConfigurationSection(id);
            if (section == null) {
                QuestsXL.get().getErrors().add(new FriendlyError(id, "Section " + id + " is null", "Section is null", "Path:\n" + resolvedSource + " - " + parentSection.getCurrentPath() + "." + id));
                return Set.of();
            }
            return loadSection(component, id, section, registry, resolvedSource);
        } else if (parentSection.isList(id)) {
            List<?> entries = parentSection.getList(id);
            if (entries == null) {
                QuestsXL.get().getErrors().add(new FriendlyError(id, "List " + id + " is null", "List is null", "Path:\n" + resolvedSource + " - " + parentSection.getCurrentPath() + "." + id));
                return Set.of();
            }
            return loadList(component, id, entries, registry, resolvedSource, parentSection.getCurrentPath());
        }
        QuestsXL.get().getErrors().add(new FriendlyError(id, "Invalid format for " + id, "Expected a section or a list.", "Path:\n" + resolvedSource + " - " + parentSection.getCurrentPath() + "." + id));
        return Set.of();
    }

    private static Set<? extends QComponent> loadSection(QComponent component, String id, ConfigurationSection section, QRegistry<?> registry, String source) {
        Set<QComponent> loadables = new HashSet<>();
        for (String key : section.getKeys(false)) {
            QComponent loadable;
            String type;
            // Format: <type>: <QLineConfig>  OR  <type>: <plain value>
            if (registry.isValid(key) && section.isString(key)) {
                try {
                    loadable = registry.get(key).getClass().getDeclaredConstructor().newInstance();
                    loadable.setParent(component);
                    String raw = section.getString(key);
                    // If the value contains no key=value pairs, treat the whole string as the
                    // primary parameter named after the type (e.g. "message: Hello" → "message=Hello")
                    QLineConfig lineConfig = (raw != null && !raw.contains("="))
                            ? new QLineConfig(key + "=" + raw)
                            : new QLineConfig(raw);
                    loadable.load(lineConfig);
                    loadables.add(loadable);
                } catch (Exception e) {
                    QuestsXL.get().getErrors().add(new FriendlyError(id, "Failed to load " + key, e.getMessage(), "Path:\n" + source + " - " + section.getCurrentPath() + "." + key).addStacktrace(e.getStackTrace()));
                }
                continue;
            }
            // Format:
            // <id> or <type>:
            //  type: <type> (Optional)
            //  value: key
            if (section.isConfigurationSection(key)) {
                ConfigurationSection subsection = section.getConfigurationSection(key);
                if (subsection == null) {
                    continue;
                }
                QConfigurationSection qConfigurationSection = new QConfigurationSection(subsection, source);
                if (subsection.contains("type")) {
                    type = subsection.getString("type");
                } else {
                    type = subsection.getName();
                }
                if (type == null) {
                    QuestsXL.get().getErrors().add(new FriendlyError(id, "Unknown type", type, "Path:\n" + source + " - " + section.getCurrentPath() + "." + key));
                    continue;
                }
                if (registry.isValid(type)) {
                    try {
                        loadable = registry.get(type).getClass().getDeclaredConstructor().newInstance();
                        loadable.setParent(component);
                        loadable.load(qConfigurationSection);
                        loadables.add(loadable);
                    } catch (Exception e) {
                        QuestsXL.get().getErrors().add(new FriendlyError(id, "Failed to load " + type + " " + key, e.getMessage(), "Path:\n" + source + " - " + section.getCurrentPath() + "." + key).addStacktrace(e.getStackTrace()));
                    }
                } else {
                    QuestsXL.get().getErrors().add(new FriendlyError(id, "Unknown type: " + type, type, "Path:\n" + source + " - " + section.getCurrentPath() + "." + key + "\n Maybe the type is not loaded in the registry."));
                }
                continue;
            }
            QuestsXL.get().getErrors().add(new FriendlyError(id, "Unknown type: " + key + " (Format: MultiLine)", key, "Path:\n" + source + " - " + section.getCurrentPath() + "." + key + "\n Maybe the type is not loaded in the registry."));
        }
        return loadables;
    }

    // Format:
    // <id>:
    //   - <type>: <QLineConfig>
    //   - <type>: <QLineConfig>
    //  ...
    private static Set<? extends QComponent> loadList(QComponent component, String id, List<?> entries, QRegistry<?> registry, String source, String parentPath) {
        Set<QComponent> loadables = new HashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            Object entry = entries.get(i);
            if (entry instanceof String s) {
                loadStringListEntry(component, id, registry, source, loadables, s);
                continue;
            }
            if (entry instanceof Map<?, ?> map) {
                loadMapListEntry(component, id, registry, source, parentPath, loadables, map, i);
                continue;
            }
            QuestsXL.get().getErrors().add(new FriendlyError(id, "Invalid list entry", String.valueOf(entry), "Path:\n" + source + " - " + parentPath + "." + id + "[" + i + "]"));
        }
        return loadables;
    }

    private static void loadStringListEntry(QComponent component, String id, QRegistry<?> registry, String source, Set<QComponent> loadables, String s) {
        if (s == null || s.isBlank()) {
            QuestsXL.get().getErrors().add(new FriendlyError(id, "Invalid list entry", "Entry is empty", "Path:\n" + source + " - " + id));
            return;
        }
        String[] split = s.split(":", 2);
        String type = split[0].trim();
        String params = split.length > 1 ? split[1] : "";
        QComponent loadable;
        if (registry.isValid(type)) {
            try {
                loadable = registry.get(type).getClass().getDeclaredConstructor().newInstance();
                loadable.setParent(component);
                loadable.load(new QLineConfig(params));
                loadables.add(loadable);
            } catch (Exception e) {
                QuestsXL.get().getErrors().add(new FriendlyError(id, "Failed to load " + type, e.getMessage(), "Path:\n" + source + " - " + s).addStacktrace(e.getStackTrace()));
            }
            return;
        }
        QuestsXL.get().getErrors().add(new FriendlyError(id, "Unknown type: " + s + "(Format: SingleLine)", s, "Path:\n" + source + " - " + id + "\n Maybe the type is not loaded in the registry."));
    }

    private static void loadMapListEntry(QComponent component, String id, QRegistry<?> registry, String source, String parentPath, Set<QComponent> loadables, Map<?, ?> map, int index) {
        if (map.isEmpty()) {
            QuestsXL.get().getErrors().add(new FriendlyError(id, "Invalid map entry", "Entry is empty", "Path:\n" + source + " - " + parentPath + "." + id + "[" + index + "]"));
            return;
        }
        if (map.size() != 1) {
            QuestsXL.get().getErrors().add(new FriendlyError(id, "Invalid map entry", "Expected exactly one type key", "Path:\n" + source + " - " + parentPath + "." + id + "[" + index + "]"));
            return;
        }

        Map.Entry<?, ?> only = map.entrySet().iterator().next();
        String type = String.valueOf(only.getKey());
        Object value = only.getValue();
        if (!registry.isValid(type)) {
            QuestsXL.get().getErrors().add(new FriendlyError(id, "Unknown type: " + type + " (Format: ListMap)", type, "Path:\n" + source + " - " + parentPath + "." + id + "[" + index + "]\n Maybe the type is not loaded in the registry."));
            return;
        }

        QComponent loadable;
        try {
            loadable = registry.get(type).getClass().getDeclaredConstructor().newInstance();
            loadable.setParent(component);
            if (value instanceof Map<?, ?> valueMap) {
                MemoryConfiguration memoryConfiguration = new MemoryConfiguration();
                ConfigurationSection tempSection = memoryConfiguration.createSection(type);
                for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
                    tempSection.set(String.valueOf(entry.getKey()), entry.getValue());
                }
                loadable.load(new QConfigurationSection(tempSection, source));
            } else {
                String raw = value == null ? "" : String.valueOf(value);
                QLineConfig lineConfig = raw.contains("=")
                        ? new QLineConfig(raw)
                        : new QLineConfig(type + "=" + raw);
                loadable.load(lineConfig);
            }
            loadables.add(loadable);
        } catch (Exception e) {
            QuestsXL.get().getErrors().add(new FriendlyError(id, "Failed to load " + type, e.getMessage(), "Path:\n" + source + " - " + parentPath + "." + id + "[" + index + "]").addStacktrace(e.getStackTrace()));
        }
    }


}
