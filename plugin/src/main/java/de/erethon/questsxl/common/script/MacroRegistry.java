package de.erethon.questsxl.common.script;

import de.erethon.questsxl.QuestsXL;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds named macro templates loaded from the shared macros/ directory.
 * Per-file macros are merged on top when processing individual quest/event files.
 */
public class MacroRegistry {

    /** macro name -> raw YAML section representing the macro body */
    private final Map<String, ConfigurationSection> globalMacros = new HashMap<>();

    /**
     * Loads all *.yml files from the given directory.
     * Each top-level key in those files is registered as a macro.
     */
    public void loadFromDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) return;
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String key : cfg.getKeys(false)) {
                if (cfg.isConfigurationSection(key)) {
                    globalMacros.put(key, cfg.getConfigurationSection(key));
                    QuestsXL.log("[MacroRegistry] Loaded global macro: " + key + " from " + file.getName());
                }
            }
        }
    }

    /** Returns all loaded global macros. */
    public Map<String, ConfigurationSection> getGlobalMacros() {
        return globalMacros;
    }
}

