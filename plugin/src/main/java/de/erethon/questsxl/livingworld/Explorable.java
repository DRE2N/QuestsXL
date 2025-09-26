package de.erethon.questsxl.livingworld;

import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.Location;

/**
 * Marker interface for objects that can be discovered by players.
 */
public interface Explorable {

    // The ID of the explorable. Should be unique within the set.
    String id();

    // The name of the explorable
    QTranslatable displayName();

    // The location of the explorable
    Location location();

    // The description of the explorable (optional)
    default QTranslatable description() {
        return null;
    }

    // Some explorables might not count for progress, like a random loot chest
    default boolean countsForProgress() {
        return true;
    }
}
