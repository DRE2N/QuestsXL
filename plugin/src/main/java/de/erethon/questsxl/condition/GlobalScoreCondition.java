package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "global_score",
        description = "Checks if a certain global score is larger or equal to a value. Global scores are server-wide.",
        shortExample = "global_score: score=example_score; value=5",
        longExample = {
                "global_score:",
                "  score: example_score",
                "  value: 5",
        }
)
public class GlobalScoreCondition extends QBaseCondition {

    @QParamDoc(name = "score", description = "The name of the global score.", required = true)
    String score;
    @QParamDoc(name = "value", description = "The value the score should be larger or equal to.", def = "1")
    int value;

    @Override
    public boolean check(QPlayer player) {
        if (QuestsXL.getInstance().getScore(score) >= value) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public boolean check(QEvent event) {
        if (QuestsXL.getInstance().getScore(score) >= value) {
            return success(event);
        }
        return fail(event);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        score = cfg.getString("score");
        value = cfg.getInt("value", 1);
    }

}
