package de.erethon.questsxl.animation;

import de.erethon.aether.animation.EntityAnimation;
import org.bukkit.configuration.ConfigurationSection;

public class QMobAnimation extends QAnimationPart {

    String mobID;
    boolean instanced;
    EntityAnimation animation;

    @Override
    public void play() {

    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        mobID = section.getString("mob");
        instanced = section.getBoolean("instanced", true);
        animation = EntityAnimation.valueOf(section.getString("animation").toUpperCase());
    }
}
