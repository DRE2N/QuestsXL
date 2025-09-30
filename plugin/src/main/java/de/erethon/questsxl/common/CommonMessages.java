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
        registerMessage("explorable.discovered", Locale.ENGLISH, QuestsXL.EXPLORATION + "<dark_purple>You discovered <arg:0>!");
        registerMessage("explorable.discovered", Locale.GERMAN, QuestsXL.EXPLORATION + "<dark_purple>Du hast <arg:0><purple>entdeckt!");
        registerMessage("explorable.undiscovered", Locale.ENGLISH, QuestsXL.EXPLORATION + "<gray>Undiscovered");
        registerMessage("explorable.undiscovered", Locale.GERMAN, QuestsXL.EXPLORATION + "<gray>Unentdeckt");
        registerMessage("explorable.respawn.nearby", Locale.ENGLISH, "<yellow>⌂ <dark_gray>- <dark_purple><arg:0>");
        registerMessage("explorable.respawn.nearby", Locale.GERMAN, "<yellow>⌂ <dark_gray>- <dark_purple>");
        registerMessage("qxl.explorable.poi.discovered", Locale.ENGLISH, "<dark_purple>You discovered a point of interest: <yellow><arg:0><dark_purple>!");
        registerMessage("qxl.explorable.poi.discovered", Locale.GERMAN, "<dark_purple>Du hast eine Sehenswürdigkeit  entdeckt: <yellow><arg:0><dark_purple>!");
        registerMessage("explorable.respawn.unlocked", Locale.ENGLISH, "<dark_purple>Unlocked waypoint: <yellow>⌂<dark_purple> <arg:0>");
        registerMessage("explorable.respawn.unlocked", Locale.GERMAN, "<dark_purple>Wegpunkt freigeschaltet: <yellow>⌂<dark_purple> <arg:0>");
        registerMessage("explorable.lootchest.nearby", Locale.ENGLISH, "<dark_purple>Loot chest: <yellow>\uD83C\uDF81<dark_purple><arg:0>");
        registerMessage("explorable.lootchest.nearby", Locale.GERMAN, "<dark_purple>Loot-Kiste: <yellow>\uD83C\uDF81<dark_purple><arg:0>");
        registerMessage("explorationset.completed", Locale.ENGLISH, QuestsXL.EXPLORATION + "<dark_purple>You completed <arg:0>!");
        registerMessage("explorationset.completed", Locale.GERMAN, QuestsXL.EXPLORATION + "<dark_purple>Du hast <arg:0> <purple>abgeschlossen!");
        registerMessage("qxl.respawn.location", Locale.ENGLISH, QuestsXL.EXPLORATION + "<gray>Respawning at <arg:0>");
        registerMessage("qxl.respawn.location", Locale.GERMAN, QuestsXL.EXPLORATION + "<gray>Respawne bei <arg:0>");

        // Exploration GUI messages
        registerMessage("gui.exploration.title", Locale.ENGLISH, "Exploration");
        registerMessage("gui.exploration.title", Locale.GERMAN, "Erkundung");
        registerMessage("gui.exploration.set.progress", Locale.ENGLISH, "<gray>Progress: <white><arg:0>/<arg:1>");
        registerMessage("gui.exploration.set.progress", Locale.GERMAN, "<gray>Fortschritt: <white><arg:0>/<arg:1>");
        registerMessage("gui.exploration.set.distance", Locale.ENGLISH, "<gray>Distance: <yellow><arg:0>m");
        registerMessage("gui.exploration.set.distance", Locale.GERMAN, "<gray>Entfernung: <yellow><arg:0>m");
        registerMessage("gui.exploration.set.completed", Locale.ENGLISH, "<green>✓ Completed");
        registerMessage("gui.exploration.set.completed", Locale.GERMAN, "<green>✓ Abgeschlossen");
        registerMessage("gui.exploration.set.clicktoview", Locale.ENGLISH, "<gray>Click to view explorables");
        registerMessage("gui.exploration.set.clicktoview", Locale.GERMAN, "<gray>Klicke um Erkundungen anzusehen");
        registerMessage("gui.exploration.back", Locale.ENGLISH, "<gray>Back to Sets");
        registerMessage("gui.exploration.back", Locale.GERMAN, "<gray>Zurück zu den Sets");
        registerMessage("gui.exploration.explorable.discovered", Locale.ENGLISH, "<green>✓ Discovered");
        registerMessage("gui.exploration.explorable.discovered", Locale.GERMAN, "<green>✓ Entdeckt");
        registerMessage("gui.exploration.explorable.undiscovered", Locale.ENGLISH, "<red>✗ Undiscovered");
        registerMessage("gui.exploration.explorable.undiscovered", Locale.GERMAN, "<red>✗ Unentdeckt");


        for (Map.Entry<String, Map<Locale, String>> entry : messages.entrySet()) {
            new QTranslatable(entry.getKey(), entry.getValue());
        }
    }

    private void registerMessage(String key, Locale locale, String message) {
        messages.computeIfAbsent(key, s -> new HashMap<>()).putIfAbsent(locale, message);
    }
}
