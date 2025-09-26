package de.erethon.questsxl.respawn;

/**
 * Defines how a respawn point can be unlocked by players
 */
public enum RespawnPointUnlockMode {
    /**
     * Unlocked by being near the respawn point (exploration-based)
     */
    NEAR,

    /**
     * Unlocked through a specific action
     */
    ACTION,

    /**
     * Unlocked when player has a specific quest
     */
    QUEST
}
