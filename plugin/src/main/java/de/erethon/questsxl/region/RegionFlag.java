package de.erethon.questsxl.region;

public enum RegionFlag {
    PROTECTED, // Can not build/destroy
    INVINCIBLE, // is invincible from damage
    INVISIBLE, // Players in the region are invisible to players outside of the region
    INSTANCED // All players outside the quest group are invisible
}