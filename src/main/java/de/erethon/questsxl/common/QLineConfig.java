package de.erethon.questsxl.common;

import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.objective.QObjective;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class QLineConfig implements QConfig {

    private final Map<String, String> result = new HashMap<>();
    private final String input;

    public QLineConfig(String string) {
        input = string;
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
    public Set<QAction> getActions(String path) {
        return Set.of();
    }

    @Override
    public Set<QCondition> getConditions(String path) {
        return Set.of();
    }

    @Override
    public Set<QObjective> getObjectives(String path) {
        return Set.of();
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
        return Integer.parseInt(result.get(key));
    }

    public int getInt(String key, int def) {
        return Integer.parseInt(result.getOrDefault(key, String.valueOf(def)));
    }

    public double getDouble(String key) {
        return Double.parseDouble(result.get(key));
    }

    public double getDouble(String key, double def) {
        return Double.parseDouble(result.getOrDefault(key, String.valueOf(def)));
    }

    public long getLong(String key) {
        return Long.parseLong(result.get(key));
    }

    public long getLong(String key, long def) {
        return Long.parseLong(result.getOrDefault(key, String.valueOf(def)));
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
}
