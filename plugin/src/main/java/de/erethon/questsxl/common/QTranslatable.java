package de.erethon.questsxl.common;

import de.erethon.questsxl.QuestsXL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class QTranslatable {

    private final String key;
    private final Map<Locale, String> translations;

    public QTranslatable(String key, Map<Locale, String> translations) {
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

    public Map<Locale, String> getTranslations() {
        return translations;
    }

    public static QTranslatable fromString(String str) {
        Map<Locale, String> translations = new HashMap<>();
        Locale defaultLocale = Locale.ENGLISH;
        if (!str.contains(";")) {
            // Simple message
            translations.put(defaultLocale, str);
            return new QTranslatable(str, translations);
        } else {
            // Complex message
            String[] split = str.split(";");
            for (String part : split) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length == 2) {
                    String localeStr = keyValue[0].trim();
                    Locale locale = Locale.forLanguageTag(localeStr);
                    translations.put(locale, keyValue[1].trim());
                }
            }
            return new QTranslatable(translations.getOrDefault(Locale.ENGLISH, "default_key"), translations);
        }
    }

}
