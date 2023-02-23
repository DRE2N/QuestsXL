package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.animation.AnimationManager;
import de.erethon.questsxl.animation.QCutscene;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class PlayCutsceneAction extends QBaseAction {

    AnimationManager manager = QuestsXL.getInstance().getAnimationManager();

    QCutscene cutscene;

    @Override
    public void play(QPlayer player) {
        cutscene.play(player.getPlayer(), this);
    }

    @Override
    public void onFinish(QPlayer player) {
        super.onFinish(player);
    }

    @Override
    public void load(QLineConfig cfg) {
        cutscene = manager.getCutscene(cfg.getString("id"));
        if (cutscene == null) {
            throw  new RuntimeException("Cutscene " + cfg.getString("id") + " does not exist.");
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        cutscene = manager.getCutscene(section.getString("id"));
        if (cutscene == null) {
            throw  new RuntimeException("Cutscene " + section.getString("id") + " does not exist.");
        }
    }
}
