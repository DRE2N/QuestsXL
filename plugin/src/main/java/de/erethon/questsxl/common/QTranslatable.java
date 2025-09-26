package de.erethon.questsxl.common;

import de.erethon.questsxl.QuestsXL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class QTranslatable {

    private static final Map<String, QTranslatable> STRING_CACHE = new HashMap<>();

    private final String key;
    private final Map<Locale, String> translations;
    private final String literal;

    private Component cachedLiteralComponent;

    public QTranslatable(String key, Map<Locale, String> translations) {
        this.key = key;
        this.translations = translations == null ? new HashMap<>() : translations;
        this.literal = null;
        if (!this.translations.isEmpty()) {
            QuestsXL.get().registerTranslation(this);
        }
    }

    private QTranslatable(String literal) {
        this.key = null;
        this.translations = new HashMap<>();
        this.literal = literal;
    }

    public Component get() {
        if (key != null) {
            return Component.translatable(key);
        }
        if (cachedLiteralComponent == null) {
            cachedLiteralComponent = MiniMessage.miniMessage().deserialize(literal == null ? "" : literal);
        }
        return cachedLiteralComponent;
    }

    public String getKey() {
        return key;
    }

    public Map<Locale, String> getTranslations() {
        return translations;
    }

    public static QTranslatable fromString(String str) {
        if (str == null) {
            return new QTranslatable("");
        }
        if (STRING_CACHE.containsKey(str)) {
            return STRING_CACHE.get(str);
        }
        Map<Locale, String> parsed = new HashMap<>();
        boolean hasLocaleEntries = false;
        String[] parts = str.split(";");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                String left = kv[0].trim();
                String right = kv[1].trim();
                // if left looks like a language tag (e.g., en or de-DE), treat as locale entry
                if (left.matches("[a-zA-Z]{2,3}(-[a-zA-Z]{2,3})?")) {
                    Locale locale = Locale.forLanguageTag(left);
                    parsed.put(locale, right);
                    hasLocaleEntries = true;
                }
            }
        }
        QTranslatable translatable;
        if (hasLocaleEntries && !parsed.isEmpty()) {
            String syntheticKey = "qxl.dynamic." + Integer.toHexString(str.hashCode());
            translatable = new QTranslatable(syntheticKey, parsed);
        } else {
            translatable = new QTranslatable(str);
        }
        STRING_CACHE.put(str, translatable);
        return translatable;
    }

    @Override
    public String toString() {
        // Return the same string that was used to create this QTranslatable
        if (key != null) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Locale, String> entry : translations.entrySet()) {
                if (!sb.isEmpty()) {
                    sb.append("; ");
                }
                sb.append(entry.getKey().toLanguageTag()).append("=").append(entry.getValue());
            }
            return sb.toString();
        }
        return literal == null ? "" : literal;
    }
}
