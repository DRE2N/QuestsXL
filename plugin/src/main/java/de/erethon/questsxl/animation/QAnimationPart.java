package de.erethon.questsxl.animation;

import de.erethon.questsxl.QuestsXL;
import org.bukkit.configuration.ConfigurationSection;

public abstract class QAnimationPart {

    QuestsXL qxl = QuestsXL.get();

    int start;
    boolean isFinished = false;

    public void play() {}

    public void load(ConfigurationSection section) {
        start = section.getInt("start", 0);
    }
}
