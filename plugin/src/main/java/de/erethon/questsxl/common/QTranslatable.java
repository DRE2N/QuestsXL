package de.erethon.questsxl.common;

import de.erethon.questsxl.QuestsXL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

import java.util.HashMap;
import java.util.Map;

public class QTranslatable {

    private final String key;
    private final Map<String, String> translations;

    public QTranslatable(String key, Map<String, String> translations) {
        this.key = key;
        this.translations = translations;
        QuestsXL.getInstance().registerTranslation(this);
    }

    public TranslatableComponent get() {
        return Component.translatable(key);
    }

    public String getKey() {
        return key;
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public static QTranslatable fromString(String str) {
        Map<String, String> translations = new HashMap<>();
        if (!str.contains(";")) {
            // Simple message
            translations.put("default", str);
            return new QTranslatable(str, translations);
        } else {
            // Complex message
            String[] split = str.split(";");
            for (String part : split) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length == 2) {
                    translations.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
            return new QTranslatable(translations.getOrDefault("default", "default_key"), translations);
        }
    }

}
