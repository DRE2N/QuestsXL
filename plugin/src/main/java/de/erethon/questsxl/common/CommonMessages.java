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

        // Daily Quest messages
        registerMessage("daily.disabled", Locale.ENGLISH, "<gray>Daily quests are currently disabled.");
        registerMessage("daily.disabled", Locale.GERMAN, "<gray>Tägliche Quests sind derzeit deaktiviert.");
        registerMessage("daily.none", Locale.ENGLISH, "<gray>No daily quests are currently available.");
        registerMessage("daily.none", Locale.GERMAN, "<gray>Keine täglichen Quests sind derzeit verfügbar.");
        registerMessage("daily.header", Locale.ENGLISH, "<dark_gray><st>               </st> <yellow><bold>Daily Quests</bold> <dark_gray><st>               </st>");
        registerMessage("daily.header", Locale.GERMAN, "<dark_gray><st>               </st> <yellow><bold>Tägliche Quests</bold> <dark_gray><st>               </st>");
        registerMessage("daily.progress", Locale.ENGLISH, "<gray>Progress: <green><arg:0><gray>/<green><arg:1>");
        registerMessage("daily.progress", Locale.GERMAN, "<gray>Fortschritt: <green><arg:0><gray>/<green><arg:1>");
        registerMessage("daily.progress.complete", Locale.ENGLISH, "<gray>Progress: <green><arg:0><gray>/<green><arg:1> <green>✓ Completed!");
        registerMessage("daily.progress.complete", Locale.GERMAN, "<gray>Fortschritt: <green><arg:0><gray>/<green><arg:1> <green>✓ Abgeschlossen!");
        registerMessage("daily.resets", Locale.ENGLISH, "<gray>Resets in: <yellow><arg:0>");
        registerMessage("daily.resets", Locale.GERMAN, "<gray>Erneuert in: <yellow><arg:0>");
        registerMessage("daily.bonus.claimed", Locale.ENGLISH, "<green><bold>✓</bold> <green>All daily quests completed! Bonus rewards claimed!");
        registerMessage("daily.bonus.claimed", Locale.GERMAN, "<green><bold>✓</bold> <green>Alle täglichen Quests abgeschlossen! Bonusbelohnungen erhalten!");

        // Weekly Quest messages
        registerMessage("weekly.disabled", Locale.ENGLISH, "<gray>Weekly quests are currently disabled.");
        registerMessage("weekly.disabled", Locale.GERMAN, "<gray>Wöchentliche Quests sind derzeit deaktiviert.");
        registerMessage("weekly.none", Locale.ENGLISH, "<gray>No weekly quests are currently available.");
        registerMessage("weekly.none", Locale.GERMAN, "<gray>Keine wöchentlichen Quests sind derzeit verfügbar.");
        registerMessage("weekly.header", Locale.ENGLISH, "<dark_gray><st>               </st> <light_purple><bold>Weekly Quests</bold> <dark_gray><st>               </st>");
        registerMessage("weekly.header", Locale.GERMAN, "<dark_gray><st>               </st> <light_purple><bold>Wöchentliche Quests</bold> <dark_gray><st>               </st>");
        registerMessage("weekly.progress", Locale.ENGLISH, "<gray>Progress: <green><arg:0><gray>/<green><arg:1>");
        registerMessage("weekly.progress", Locale.GERMAN, "<gray>Fortschritt: <green><arg:0><gray>/<green><arg:1>");
        registerMessage("weekly.progress.complete", Locale.ENGLISH, "<gray>Progress: <green><arg:0><gray>/<green><arg:1> <green>✓ Completed!");
        registerMessage("weekly.progress.complete", Locale.GERMAN, "<gray>Fortschritt: <green><arg:0><gray>/<green><arg:1> <green>✓ Abgeschlossen!");
        registerMessage("weekly.resets", Locale.ENGLISH, "<gray>Resets in: <yellow><arg:0>");
        registerMessage("weekly.resets", Locale.GERMAN, "<gray>Erneuert in: <yellow><arg:0>");
        registerMessage("weekly.bonus.claimed", Locale.ENGLISH, "<green><bold>✓</bold> <green>All weekly quests completed! Bonus rewards claimed!");
        registerMessage("weekly.bonus.claimed", Locale.GERMAN, "<green><bold>✓</bold> <green>Alle wöchentlichen Quests abgeschlossen! Bonusbelohnungen erhalten!");

        // Periodic Quest reset broadcasts
        registerMessage("daily.reset.broadcast", Locale.ENGLISH, "<yellow><bold>Daily quests have reset!");
        registerMessage("daily.reset.broadcast", Locale.GERMAN, "<yellow><bold>Tägliche Quests wurden erneuert!");
        registerMessage("weekly.reset.broadcast", Locale.ENGLISH, "<yellow><bold>Weekly quests have reset!");
        registerMessage("weekly.reset.broadcast", Locale.GERMAN, "<yellow><bold>Wöchentliche Quests wurden erneuert!");

        // Periodic Quest bonus notifications (sent to individual player)
        registerMessage("daily.bonus.received", Locale.ENGLISH, "<green><bold>You completed all daily quests and received bonus rewards!");
        registerMessage("daily.bonus.received", Locale.GERMAN, "<green><bold>Du hast alle täglichen Quests abgeschlossen und Bonusbelohnungen erhalten!");
        registerMessage("weekly.bonus.received", Locale.ENGLISH, "<green><bold>You completed all weekly quests and received bonus rewards!");
        registerMessage("weekly.bonus.received", Locale.GERMAN, "<green><bold>Du hast alle wöchentlichen Quests abgeschlossen und Bonusbelohnungen erhalten!");

        // Periodic Quest system messages
        registerMessage("periodic.system.disabled", Locale.ENGLISH, QuestsXL.ERROR + "Periodic quest system is not enabled.");
        registerMessage("periodic.system.disabled", Locale.GERMAN, QuestsXL.ERROR + "Das periodische Quest-System ist nicht aktiviert.");

        for (Map.Entry<String, Map<Locale, String>> entry : messages.entrySet()) {
            new QTranslatable(entry.getKey(), entry.getValue());
        }
    }

    private void registerMessage(String key, Locale locale, String message) {
        messages.computeIfAbsent(key, s -> new HashMap<>()).putIfAbsent(locale, message);
    }
}
