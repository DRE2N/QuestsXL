package de.erethon.questsxl.common;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public class QLineConfig {

    private final Map<String, String> result = new HashMap<>();
    private final String input;

    public QLineConfig(String string) {
        input = string;
        parse();
    }

    private void parse() {
        String[] pairs = input.split(";");
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

    public String getString(String key) {
        return result.get(key);
    }

    public String getString(String key, String def) {
        return result.getOrDefault(key, def);
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

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(result.get(key));
    }

    public boolean getBoolean(String key, boolean def) {
        return Boolean.parseBoolean(result.getOrDefault(key, String.valueOf(def)));
    }

    public String[] getArray(String key) {
        return result.get(key).split(",");
    }

    public String[] getArray(String key, String def) {
        return result.getOrDefault(key, def).split(",");
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
