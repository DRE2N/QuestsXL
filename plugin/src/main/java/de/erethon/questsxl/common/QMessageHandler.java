package de.erethon.questsxl.common;

import de.erethon.bedrock.chat.MiniMessageTranslator;
import de.erethon.questsxl.QuestsXL;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class QMessageHandler extends MiniMessageTranslator {

    private final Key translatorKey;
    private final Map<String, Map<Locale, String>> translations = new HashMap<>();

    GlobalTranslator globalTranslator = GlobalTranslator.translator();

    public QMessageHandler() {
        this.translatorKey = Key.key("qxl", "main");
        globalTranslator.addSource(this);
    }

    @Override
    protected @Nullable String getMiniMessageString(@NotNull String key, @NotNull Locale locale) {
        Map<Locale, String> map = translations.get(key);
        if (map == null) return null;
        String value = map.get(locale);
        if (value != null) return value;
        Locale langOnly = Locale.forLanguageTag(locale.getLanguage());
        value = map.get(langOnly);
        if (value != null) return value;
        value = map.get(Locale.ENGLISH);
        if (value != null) return value;
        return map.values().stream().findFirst().orElse(null);
    }

    private String toTranslationPath(String path) {
        if (path == null) return null;
        String prefix = translatorKey.namespace() + ".";
        if (path.startsWith(prefix)) {
            return path;
        }
        return prefix + path;
    }

    public void registerTranslation(QTranslatable translatable) {
        String key = translatable.getKey();
        if (key == null) return;
        String translationPath = key.startsWith("qxl.") ? key : toTranslationPath(key);

        boolean isNewTranslation = !translations.containsKey(translationPath);

        for (Map.Entry<Locale, String> entry : translatable.getTranslations().entrySet()) {
            translations
                    .computeIfAbsent(translationPath, s -> new HashMap<>())
                    .putIfAbsent(entry.getKey(), entry.getValue());
        }

        if (isNewTranslation) {
            QuestsXL.log("Registered translation: " + translationPath + " with the following locales: " + translatable.getTranslations().keySet());
            QuestsXL.log("Translations for " + translationPath + ": " + translations.get(translationPath));
        }
    }

    @Override
    public @NotNull Key name() {
        return translatorKey;
    }

    @Override
    public @NotNull TriState hasAnyTranslations() {
        return translations.isEmpty() ? TriState.FALSE : TriState.TRUE;
    }
}
