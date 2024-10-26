package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.animation.AnimationManager;
import de.erethon.questsxl.animation.QCutscene;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "play_cutscene",
        description = "Plays a cutscene. The cutscene needs to be created first.",
        shortExample = "- 'play_cutscene: id=example_cutscene'",
        longExample = {
                "play_cutscene:",
                "  id: example_cutscene",
        }
)
public class PlayCutsceneAction extends QBaseAction {

    AnimationManager manager = QuestsXL.getInstance().getAnimationManager();

    @QParamDoc(name = "id", description = "The ID of the cutscene to play", required = true)
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
    public void load(QConfig cfg) {
        super.load(cfg);
        cutscene = manager.getCutscene(cfg.getString("id"));
        if (cutscene == null) {
            throw  new RuntimeException("Cutscene " + cfg.getString("id") + " does not exist.");
        }
    }
}
