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
            if (registry.isValid(key)) {
                try {
                    loadable = registry.get(key).getClass().getDeclaredConstructor().newInstance();
                    loadable.load(new QLineConfig(section.getString(key)));
                    loadables.add(loadable);
                } catch (Exception e) {
                    QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Failed to load " + key, e.getMessage(), "Pfad:\n" + section.getCurrentPath() + "." + key).addStacktrace(e.getStackTrace()));
                    e.printStackTrace();
                }
                continue;
            }
            // Format:
            // blabla:
            //  type: <type>
            //  value: key
            if (section.isConfigurationSection(key)) {
                ConfigurationSection subsection = section.getConfigurationSection(key);
                if (subsection == null) {
                    continue;
                }
                if (subsection.contains("type")) {
                    type = subsection.getString("type");
                }
                if (type == null) {
                    QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Unknown type", type, "Pfad:\n" + section.getCurrentPath() + "." + key));
                    continue;
                }
                if (registry.isValid(type)) {
                    try {
                        loadable = registry.get(type).getClass().getDeclaredConstructor().newInstance();
                        loadable.load(subsection);
                        loadables.add(loadable);
                    } catch (Exception e) {
                        QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Failed to load " + type + " " + key, e.getMessage(), "Pfad:\n" + section.getCurrentPath() + "." + key).addStacktrace(e.getStackTrace()));
                        e.printStackTrace();
                    }
                }
            }
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
            if (registry.isValid(type))
                try {
                    loadable = registry.get(type).getClass().getDeclaredConstructor().newInstance();
                    loadable.load(new QLineConfig(s.replace(type + ":", "")));
                    loadables.add(loadable);
                } catch (Exception e) {
                    QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Failed to load " + type, e.getMessage(), "Pfad:\n" + s).addStacktrace(e.getStackTrace()));
                    e.printStackTrace();
                }
        }
        return loadables;
    }

}
