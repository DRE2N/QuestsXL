package de.erethon.questsxl.common;

import de.erethon.bedrock.chat.MiniMessageTranslator;
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
        this.translatorKey = Key.key("qxl");
        globalTranslator.addSource(this);
    }

    @Override
    protected @Nullable String getMiniMessageString(@NotNull String key, @NotNull Locale locale) {
        Map<Locale, String> map = translations.get(key);
        if (map == null) return null;
        String value = map.get(locale);
        if (value != null) return value;
        Locale langOnly = new Locale(locale.getLanguage());
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
        String translationPath = toTranslationPath(translatable.getKey());
        if (translationPath == null) return;
        for (Map.Entry<Locale, String> entry : translatable.getTranslations().entrySet()) {
            translations
                    .computeIfAbsent(translationPath, s -> new HashMap<>())
                    .putIfAbsent(entry.getKey(), entry.getValue());
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
