package de.erethon.questsxl.condition;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "player_score",
        description = "Checks if a player has at least a certain score. Per-player",
        shortExample = "player_score: score=score_name; value=5",
        longExample = {
                "player_score:",
                "  score: score_name",
                "  value: 5"
        }
)
public class PlayerScoreCondition extends QBaseCondition {

    @QParamDoc(name = "score", description = "The name of the score.", required = true)
    private String score;
    @QParamDoc(name = "value", description = "The value the score should be larger or equal to.", def = "1")
    private int value;
    @QParamDoc(name = "mode", description = "The mode of the condition. Can be 'at_least' or 'at_most', 'exactly' or 'unset'.", def = "at_least")
    private ScoreConditionMode mode;

    @Override
    public boolean check(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return check(quester);
        }
        switch (mode) {
            case AT_LEAST:
                if (QuestsXL.get().getScore((score)) >= value) {
                    return success(quester);
                }
                break;
            case AT_MOST:
                if (QuestsXL.get().getScore((score)) <= value) {
                    return success(quester);
                }
                break;
            case EXACTLY:
                if (QuestsXL.get().getScore((score)) == value) {
                    return success(quester);
                }
                break;
            case UNSET:
                if (QuestsXL.get().getScore((score)) == 0) {
                    return success(quester);
                }
                break;
        }
        return fail(player);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        score = cfg.getString("score");
        value = cfg.getInt("value");
        mode = ScoreConditionMode.valueOf(cfg.getString("mode", "at_least").toUpperCase());
    }
}
