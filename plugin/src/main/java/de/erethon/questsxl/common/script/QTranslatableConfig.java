package de.erethon.questsxl.common.script;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class QTranslatableConfig {

    private QTranslatableConfig() {
    }

    public static QTranslatable fromQLine(QLineConfig section, String field, String key, String fallback) {
        String english = section.getString(field + ".en", null);
        String german = section.getString(field + ".de", null);
        if ((english != null && !english.isBlank()) || (german != null && !german.isBlank())) {
            return fromTranslations(key, english, german);
        }
        return QTranslatable.fromString(section.getString(field, fallback));
    }

    public static void toQLine(QLineConfig section, String field, QTranslatable value) {
        if (value != null && !value.getTranslations().isEmpty()) {
            String english = value.getTranslations().get(Locale.ENGLISH);
            String german = value.getTranslations().get(Locale.GERMAN);
            if (english != null && !english.isBlank()) {
                section.set(field + ".en", english);
            }
            if (german != null && !german.isBlank()) {
                section.set(field + ".de", german);
            }
            return;
        }
        section.set(field, value == null ? "" : value.toString());
    }

    public static QTranslatable fromSection(ConfigurationSection section, String field, String key, String fallback) {
        String english = section.getString(field + ".en", null);
        String german = section.getString(field + ".de", null);
        if ((english != null && !english.isBlank()) || (german != null && !german.isBlank())) {
            return fromTranslations(key, english, german);
        }
        return QTranslatable.fromString(section.getString(field, fallback));
    }

    public static void toSection(ConfigurationSection section, String field, QTranslatable value) {
        section.set(field, null);
        if (value != null && !value.getTranslations().isEmpty()) {
            String english = value.getTranslations().get(Locale.ENGLISH);
            String german = value.getTranslations().get(Locale.GERMAN);
            if (english != null && !english.isBlank()) {
                section.set(field + ".en", english);
            }
            if (german != null && !german.isBlank()) {
                section.set(field + ".de", german);
            }
            return;
        }
        section.set(field, value == null ? null : value.toString());
    }

    private static QTranslatable fromTranslations(String key, String english, String german) {
        Map<Locale, String> translations = new HashMap<>();
        if (english != null && !english.isBlank()) {
            translations.put(Locale.ENGLISH, english);
        }
        if (german != null && !german.isBlank()) {
            translations.put(Locale.GERMAN, german);
        }
        return new QTranslatable(key, translations);
    }
}
