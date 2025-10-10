package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;

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
    private String score;
    @QParamDoc(name = "value", description = "The value the score should be larger or equal to.", def = "1")
    private int value;
    @QParamDoc(name = "mode", description = "The mode of the condition. Can be 'at_least' or 'at_most', 'exactly' or 'unset'.", def = "at_least")
    private ScoreConditionMode mode;

    @Override
    public boolean check(Quester quester) {
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
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        score = cfg.getString("score");
        value = cfg.getInt("value", 1);
        mode = ScoreConditionMode.valueOf(cfg.getString("mode", "at_least").toUpperCase());
    }

}
