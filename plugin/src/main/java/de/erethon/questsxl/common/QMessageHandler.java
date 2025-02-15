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
        return map == null ? null : map.get(locale);
    }

    private String toTranslationPath(String path) {
        return translatorKey.namespace() + "." + path;
    }

    public void registerTranslation(QTranslatable translatable) {
        String translationPath = toTranslationPath(translatable.getKey());
        for (Map.Entry<Locale, String> entry : translatable.getTranslations().entrySet()) {
            translations.computeIfAbsent(translationPath, s -> new HashMap<>()).putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public @NotNull Key name() {
        return translatorKey;
    }

    @Override
    public @NotNull TriState hasAnyTranslations() {
        return super.hasAnyTranslations();
    }
}
