package de.erethon.questsxl.common.script;

import de.erethon.questsxl.QuestsXL;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs load-time macro expansion on a YamlConfiguration before any QComponent is loaded.
 *
 * <h2>Macro definition (in the same file or in the shared macros/ directory)</h2>
 * <pre>
 * macros:
 *   launch:            # macro name
 *     velocity:        # body – an arbitrary YAML subtree
 *       x: $1
 *       y: $2
 *       z: $3
 * </pre>
 *
 * <h2>Macro invocation</h2>
 * Use {@code call_<macroName>} as a YAML key. Positional parameters are passed as a
 * comma-separated string value. The key may be followed by an optional {@code _<suffix>}
 * disambiguator so that the same macro can be called multiple times in one section.
 * <pre>
 * onStart:
 *   call_launch: "0, 5, 0"        # expands "launch" with $1=0, $2=5, $3=0
 *   call_launch_2: "1, 3, 1"      # second call to the same macro
 *   call_greet:                   # zero-arg invocation (null value is fine)
 * </pre>
 *
 * Parameters may also appear in string <em>values</em>: {@code message: "Hello $1!"}.
 */
public class MacroProcessor {

    /**
     * Matches {@code call_<macroName>} or {@code call_<macroName>_<disambiguator>}.
     * Group 1 = macro name (without any trailing _N disambiguator).
     * The value of the key (if present) is the comma-separated args string.
     */
    private static final Pattern INVOCATION_PATTERN = Pattern.compile(
            "^call_([a-zA-Z0-9_]+?)(?:_\\d+)?$"
    );

    /**
     * Processes a quest/event YamlConfiguration in-place.
     * <ol>
     *   <li>Reads (and removes) the optional {@code macros:} section from the file.</li>
     *   <li>Merges with the provided global registry (file macros override globals).</li>
     *   <li>Recursively expands {@code call_name} / {@code call_name: "args"} invocations.</li>
     * </ol>
     *
     * @param cfg    the root configuration to process (mutated in-place)
     * @param global the shared macro registry (may be null)
     */
    public static void process(YamlConfiguration cfg, MacroRegistry global) {
        // Collect all macros: globals first, then file-local (overrides)
        Map<String, ConfigurationSection> macros = new HashMap<>();
        if (global != null) {
            macros.putAll(global.getGlobalMacros());
        }
        if (cfg.isConfigurationSection("macros")) {
            ConfigurationSection macroSection = cfg.getConfigurationSection("macros");
            if (macroSection != null) {
                for (String key : macroSection.getKeys(false)) {
                    if (macroSection.isConfigurationSection(key)) {
                        macros.put(key, macroSection.getConfigurationSection(key));
                    }
                }
            }
            cfg.set("macros", null); // remove from config so it isn't treated as a component section
        }

        if (macros.isEmpty()) return;

        // Expand across the whole config
        expandSection(cfg, macros);
    }

    // ------------------------------------------------------------------
    // Recursive expansion
    // ------------------------------------------------------------------

    private static void expandSection(ConfigurationSection section, Map<String, ConfigurationSection> macros) {
        // Snapshot keys to avoid ConcurrentModificationException when we mutate the section
        Set<String> keys = new LinkedHashSet<>(section.getKeys(false));
        for (String key : keys) {
            Matcher m = INVOCATION_PATTERN.matcher(key);
            if (m.matches()) {
                String macroName = m.group(1);
                // Args are the string value of the key; null or blank means no args
                String argsRaw = section.getString(key, null);
                List<String> args = parseArgs(argsRaw);
                QuestsXL.log("[MacroProcessor] Expanding macro: call_" + macroName + " with " + args.size() + " args in section: " + section.getCurrentPath());

                ConfigurationSection template = macros.get(macroName);
                if (template == null) {
                    QuestsXL.get().getErrors().add(new de.erethon.questsxl.error.FriendlyError(
                            "MacroProcessor",
                            "Unknown macro: call_" + macroName,
                            "No macro with this name is defined.",
                            "In section: " + section.getCurrentPath()
                    ));
                    section.set(key, null); // remove the broken invocation key
                    continue;
                }

                // Remove the invocation key
                section.set(key, null);

                // Inject the expanded macro body into the section
                injectMacro(section, template, args, macros);
                continue;
            }

            // Not a macro invocation — recurse into subsections
            if (section.isConfigurationSection(key)) {
                ConfigurationSection sub = section.getConfigurationSection(key);
                if (sub != null) expandSection(sub, macros);
            } else if (section.isList(key)) {
                List<?> list = section.getList(key);
                if (list != null) {
                    List<Object> expanded = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof String s) {
                            String trimmed = s.trim();
                            Matcher lm = INVOCATION_PATTERN.matcher(trimmed);
                            if (lm.matches()) {
                                String macroName = lm.group(1);
                                ConfigurationSection template = macros.get(macroName);
                                if (template != null) {
                                    // Serialize macro body to the line-config string format
                                    expanded.add(macroToLineString(template, List.of()));
                                    continue;
                                }
                            }
                        }
                        expanded.add(item);
                    }
                    section.set(key, expanded);
                }
            }
        }
    }

    /**
     * Copies all top-level keys from the template into the target section,
     * substituting positional args ($1, $2, …) in all string values.
     * If a key already exists in the target (e.g. two macro invocations both expand
     * a "message" key), a numeric suffix (_2, _3, …) is appended automatically so
     * that all injected entries survive without overwriting each other.
     * Recursively expands any nested macro invocations in the inserted subtree.
     */
    private static void injectMacro(ConfigurationSection target, ConfigurationSection template,
                                    List<String> args, Map<String, ConfigurationSection> macros) {
        for (String templateKey : template.getKeys(false)) {
            String typeName = substituteArgs(templateKey, args);
            String resolvedKey = deduplicateKey(target, typeName);
            if (template.isConfigurationSection(templateKey)) {
                ConfigurationSection sub = template.getConfigurationSection(templateKey);
                if (sub == null) continue;
                ConfigurationSection dest = target.createSection(resolvedKey);
                copySection(sub, dest, args);
                // If the key was renamed for deduplication, inject an explicit "type" so
                // QConfigLoader can still resolve the component type correctly.
                if (!resolvedKey.equals(typeName) && !dest.contains("type")) {
                    dest.set("type", typeName);
                }
                // Recurse to handle nested macro calls inside the injected body
                expandSection(dest, macros);
            } else {
                Object value = template.get(templateKey);
                target.set(resolvedKey, substituteInValue(value, args));
            }
        }
    }

    /**
     * Returns {@code base} if that key does not yet exist in {@code section},
     * otherwise appends {@code _macroN} until a free slot is found.
     * Using {@code _macro} as separator avoids colliding with real component type names.
     * The section is also given an explicit {@code type} key so QConfigLoader resolves
     * the type from the original name, not the deduplicated key.
     */
    private static String deduplicateKey(ConfigurationSection section, String base) {
        if (!section.contains(base)) return base;
        int suffix = 2;
        while (section.contains(base + "_macro" + suffix)) suffix++;
        return base + "_macro" + suffix;
    }

    /** Deep-copies a section into dest, substituting positional args in all string values/keys. */
    private static void copySection(ConfigurationSection source, ConfigurationSection dest, List<String> args) {
        for (String key : source.getKeys(false)) {
            String resolvedKey = substituteArgs(key, args);
            if (source.isConfigurationSection(key)) {
                ConfigurationSection sub = source.getConfigurationSection(key);
                if (sub == null) continue;
                copySection(sub, dest.createSection(resolvedKey), args);
            } else {
                dest.set(resolvedKey, substituteInValue(source.get(key), args));
            }
        }
    }

    /** Converts a macro body section into the line-config string format for list entries. */
    private static String macroToLineString(ConfigurationSection template, List<String> args) {
        StringBuilder sb = new StringBuilder();
        for (String key : template.getKeys(false)) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(substituteArgs(key, args));
            if (template.isConfigurationSection(key)) {
                ConfigurationSection sub = template.getConfigurationSection(key);
                if (sub != null) {
                    for (String sk : sub.getKeys(false)) {
                        sb.append(" ").append(sk).append("=").append(substituteArgs(String.valueOf(sub.get(sk)), args));
                    }
                }
            } else {
                sb.append(": ").append(substituteArgs(String.valueOf(template.get(key)), args));
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Argument helpers
    // ------------------------------------------------------------------

    private static List<String> parseArgs(String argsRaw) {
        List<String> args = new ArrayList<>();
        if (argsRaw == null || argsRaw.isBlank()) return args;
        for (String part : argsRaw.split(",")) {
            args.add(part.strip());
        }
        return args;
    }

    private static String substituteArgs(String text, List<String> args) {
        if (text == null) return null;
        for (int i = 0; i < args.size(); i++) {
            text = text.replace("$" + (i + 1), args.get(i));
        }
        return text;
    }

    private static Object substituteInValue(Object value, List<String> args) {
        if (value instanceof String s) {
            return substituteArgs(s, args);
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                result.add(item instanceof String s ? substituteArgs(s, args) : item);
            }
            return result;
        }
        return value;
    }
}
