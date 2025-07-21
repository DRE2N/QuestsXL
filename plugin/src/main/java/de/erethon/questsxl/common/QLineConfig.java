package de.erethon.questsxl.common;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a configuration that is a single line of text.
 * Can optionally be parsed and read into/from a bukkit configuration section (not-nested).
 */
public class QLineConfig implements QConfig {

    private final Map<String, String> result = new HashMap<>();
    private final String input;

    public QLineConfig() {
        input = "";
    }

    public QLineConfig(String string) {
        input = string;
        parse();
    }

    public QLineConfig(ConfigurationSection section) {
        input = section.getName();
        for (String key : section.getKeys(false)) {
            result.put(key, section.getString(key));
        }
    }

    public QLineConfig(Map<String, Object> map) {
        input = map.toString();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof String) {
                result.put(entry.getKey(), (String) entry.getValue());
            } else {
                result.put(entry.getKey(), entry.getValue().toString());
            }
        }
        parse();
    }

    private void parse() {
        String cleanedInput = input.trim().replaceAll("\\s*=\\s*", "=").replaceAll(";\\s*", ";");// Remove spaces around = and after ;
        String[] pairs = cleanedInput.split(";");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                result.put(keyValue[0], keyValue[1]);
            }
        }
    }

    public boolean contains(String key) {
        return result.containsKey(key);
    }

    // Unused - No actions, conditions, or objectives in line configs
    @Override
    public Set<QAction> getActions(QComponent component, String path) {
        return Set.of();
    }

    @Override
    public Set<QCondition> getConditions(QComponent component, String path) {
        return Set.of();
    }

    @Override
    public Set<QObjective> getObjectives(QComponent component, String path) {
        return Set.of();
    }

    @Override
    public QEvent getQEvent(String event) {
        return QuestsXL.getInstance().getEventManager().getByID(event);
    }

    @Override
    public QQuest getQuest(String quest) {
        return QuestsXL.getInstance().getQuestManager().getByName(quest);
    }

    public String getString(String key) {
        return result.get(key);
    }

    public String getString(String key, String def) {
        return result.getOrDefault(key, def);
    }

    @Override
    public String getName() {
        return input;
    }

    public int getInt(String key) {
        return (int) Double.parseDouble(result.get(key));
    }

    public int getInt(String key, int def) {
        return (int) Double.parseDouble(result.getOrDefault(key, String.valueOf(def)));
    }

    public double getDouble(String key) {
        return Double.parseDouble(result.get(key));
    }

    public double getDouble(String key, double def) {
        return Double.parseDouble(result.getOrDefault(key, String.valueOf(def)));
    }

    public long getLong(String key) {
        return (long) Double.parseDouble(result.get(key));
    }

    public long getLong(String key, long def) {
        return (long) Double.parseDouble(result.getOrDefault(key, String.valueOf(def)));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(result.get(key));
    }

    public boolean getBoolean(String key, boolean def) {
        return Boolean.parseBoolean(result.getOrDefault(key, String.valueOf(def)));
    }

    public String[] getStringArray(String key) {
        return result.get(key).split(",");
    }

    @Override
    public String[] getStringArray(String path, String[] def) {
        if (contains(path)) {
            return getStringArray(path);
        }
        return def;
    }

    @Override
    public QLocation getQLocation(String path) {
        return new QLocation(this);
    }

    @Override
    public QLocation getQLocation(String path, QLocation def) {
        if (contains(path)) {
            return new QLocation(this);
        }
        return def;
    }

    public void set(String key, String value) {
        result.put(key, value);
    }

    public void set(String key, int value) {
        result.put(key, String.valueOf(value));
    }

    public void set(String key, double value) {
        result.put(key, String.valueOf(value));
    }

    public void set(String key, long value) {
        result.put(key, String.valueOf(value));
    }

    public void set(String key, boolean value) {
        result.put(key, String.valueOf(value));
    }

    public void set(String key, String[] value) {
        StringBuilder builder = new StringBuilder();
        for (String s : value) {
            builder.append(s).append(",");
        }
        result.put(key, builder.toString());
    }

    public World getWorld(String key) {
        return Bukkit.getWorld(result.get(key));
    }

    public World getWorld(String key, World def) {
        return Bukkit.getWorld(result.getOrDefault(key, def.getName()));
    }

    @Override
    public String toString() {
        return input;
    }

    public ConfigurationSection toConfigSection(ConfigurationSection parent) {
        ConfigurationSection section = parent.createSection(input);
        for (Map.Entry<String, String> entry : result.entrySet()) {
            section.set(entry.getKey(), entry.getValue());
        }
        return section;
    }
}
