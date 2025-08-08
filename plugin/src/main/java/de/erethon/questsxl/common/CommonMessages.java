package de.erethon.questsxl.common;

import de.erethon.questsxl.QuestsXL;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Contains common messages that are used throughout the plugin.
 * Hardcoded because I don't care enough.
 */
public class CommonMessages {

    private final Map<String, Map<Locale, String>> messages = new HashMap<>();

    public CommonMessages() {
        // Keys are prefixed with "qxl" automatically by the QMessageHandler
        registerMessage("error", Locale.ENGLISH, QuestsXL.ERROR + "<gray>An internal error occurred. Please report this to an administrator. Error: <red><arg:0> <dark_gray> | <arg:1>");
        registerMessage("error", Locale.GERMAN, QuestsXL.ERROR + "<gray>Ein interner Fehler ist aufgetreten. Bitte melde dies einem Administrator. Fehler: <red><arg:0> <dark_gray> | <arg:1>");
        registerMessage("explorable.discovered", Locale.ENGLISH, QuestsXL.EXPLORATION + "<purple>You discovered <arg:0>!");
        registerMessage("explorable.discovered", Locale.GERMAN, QuestsXL.EXPLORATION + "<purple>Du hast <arg:0><purple>entdeckt!");
        registerMessage("explorable.undiscovered", Locale.ENGLISH, QuestsXL.EXPLORATION + "<gray>Undiscovered");
        registerMessage("explorable.undiscovered", Locale.GERMAN, QuestsXL.EXPLORATION + "<gray>Unentdeckt");
        registerMessage("explorationset.completed", Locale.ENGLISH, QuestsXL.EXPLORATION + "<purple>You completed <arg:0>!");
        registerMessage("explorationset.completed", Locale.GERMAN, QuestsXL.EXPLORATION + "<purple>Du hast <arg:0> <purple>abgeschlossen!");

        for (Map.Entry<String, Map<Locale, String>> entry : messages.entrySet()) {
            QTranslatable translatable = new QTranslatable(entry.getKey(), entry.getValue());
            QuestsXL.get().registerTranslation(translatable);
        }
    }

    private void registerMessage(String key, Locale locale, String message) {
        messages.computeIfAbsent(key, s -> new HashMap<>()).putIfAbsent(locale, message);
    }
}
