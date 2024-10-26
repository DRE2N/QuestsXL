package de.erethon.questsxl.common;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QConfigLoader {

    public static @Nullable Set<? extends QLoadable> load(String id, ConfigurationSection parentSection, QRegistry<?> registry) {
        if (parentSection.isConfigurationSection(id)) {
            ConfigurationSection section = parentSection.getConfigurationSection(id);
            if (section == null) {
                return null;
            }
            return loadSection(id, section, registry);
        }
        else if (parentSection.isList(id)) {
            List<String> strings = parentSection.getStringList(id);
            return loadList(id, strings, registry);
        }
        return null;
    }

    private static Set<? extends QLoadable> loadSection(String id, ConfigurationSection section, QRegistry<?> registry) {
        Set<QLoadable> loadables = new HashSet<>();
        for (String key : section.getKeys(false)) {
            QLoadable loadable;
            String type = null;
            // Format: <type>: <QLineConfig>
            if (registry.isValid(key) && section.isString(key)) {
                try {
                    loadable = registry.get(key).getClass().getDeclaredConstructor().newInstance();
                    loadable.load(new QLineConfig(section.getString(key)));
                    loadables.add(loadable);
                } catch (Exception e) {
                    QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Failed to load " + key, e.getMessage(), "Path:\n" + section.getCurrentPath() + "." + key).addStacktrace(e.getStackTrace()));
                    e.printStackTrace();
                }
                continue;
            }
            // Format:
            // blabla or <type>:
            //  type: <type> (Optional)
            //  value: key
            if (section.isConfigurationSection(key)) {
                ConfigurationSection subsection = section.getConfigurationSection(key);
                if (subsection == null) {
                    continue;
                }
                QConfigurationSection qConfigurationSection = new QConfigurationSection(subsection);
                if (subsection.contains("type")) {
                    type = subsection.getString("type");
                } else {
                    type = subsection.getName();
                }
                if (type == null) {
                    QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Unknown type", type, "Path:\n" + section.getCurrentPath() + "." + key));
                    continue;
                }
                if (registry.isValid(type)) {
                    try {
                        loadable = registry.get(type).getClass().getDeclaredConstructor().newInstance();
                        loadable.load(qConfigurationSection);
                        loadables.add(loadable);
                    } catch (Exception e) {
                        QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Failed to load " + type + " " + key, e.getMessage(), "Path:\n" + section.getCurrentPath() + "." + key).addStacktrace(e.getStackTrace()));
                        e.printStackTrace();
                    }
                } else {
                    QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Unknown type: " + type, type, "Path:\n" + section.getCurrentPath() + "." + key + "\n Maybe the type is not loaded in the registry."));
                }
                continue;
            }
            QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Unknown type: " + key + " (Format: MultiLine)", key, "Path:\n" + section.getCurrentPath() + "." + key + "\n Maybe the type is not loaded in the registry."));
        }
        return loadables;
    }

    // Format:
    // <id>:
    //   - <type>: <QLineConfig>
    //   - <type>: <QLineConfig>
    //  ...
    private static Set<? extends QLoadable> loadList(String id, List<String> strings, QRegistry<?> registry) {
        Set<QLoadable> loadables = new HashSet<>();
        for (String s : strings) {
            String type = s.split(":")[0];
            QLoadable loadable;
            if (registry.isValid(type)) {
                MessageUtil.log("Loading " + type + " from " + s);
                try {
                    loadable = registry.get(type).getClass().getDeclaredConstructor().newInstance();
                    loadable.load(new QLineConfig(s.replace(type + ":", "")));
                    MessageUtil.log("Loadable: " + loadable);
                    loadables.add(loadable);
                } catch (Exception e) {
                    QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Failed to load " + type, e.getMessage(), "Pfad:\n" + s).addStacktrace(e.getStackTrace()));
                    e.printStackTrace();
                }
                continue;
            }
            QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Unknown type: " + s + "(Format: SingleLine)", s, "Path:\n" + id + "\n Maybe the type is not loaded in the registry."));
        }
        return loadables;
    }

}
