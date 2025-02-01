package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.animation.AnimationManager;
import de.erethon.questsxl.animation.QCutscene;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "play_cutscene",
        description = "Plays a cutscene. The cutscene needs to be created first.",
        shortExample = "play_cutscene: cutscene=example_cutscene",
        longExample = {
                "play_cutscene:",
                "  cutscene: example_cutscene",
        }
)
public class PlayCutsceneAction extends QBaseAction {

    AnimationManager manager = QuestsXL.getInstance().getAnimationManager();

    @QParamDoc(name = "cutscene", description = "The ID of the cutscene to play", required = true)
    QCutscene cutscene;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester, (QPlayer player) -> cutscene.play(player.getPlayer(), this));
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        cutscene = manager.getCutscene(cfg.getString("cutscene"));
        if (cutscene == null) {
            throw  new RuntimeException("Cutscene " + cfg.getString("id") + " does not exist.");
        }
    }
}
