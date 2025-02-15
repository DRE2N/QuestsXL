package de.erethon.questsxl.common;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an object that can use the score system.
 * Not to be confused with the Global Score - that does not require any objects that hold scores.
 */
public interface Scorable {
    void addScore(@NotNull String score, int amount);

    void removeScore(@NotNull String score, int amount);

    void setScore(@NotNull String score, int amount);

    int getScore(@NotNull String id);
}

