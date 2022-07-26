package de.erethon.questsxl.common;

import org.jetbrains.annotations.NotNull;

public interface Scorable {
    void addScore(@NotNull String score, int amount);

    void removeScore(@NotNull String score, int amount);

    void setScore(@NotNull String score, int amount);

    int getScore(@NotNull String id);
}

