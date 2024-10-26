package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "play_animation",
        description = "**Currently not implemented**"
)
public class PlayAnimationAction extends QBaseAction {

    @Override
    public void play(QPlayer player) {
        super.play(player);
    }

    @Override
    public void onFinish(QPlayer player) {
        super.onFinish(player);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
    }
}
