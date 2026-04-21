package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.player.QPlayer;

import java.util.Map;

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
public class PlayerScoreCondition extends QBaseCondition implements VariableProvider {

    @QParamDoc(name = "score", description = "The name of the score.", required = true)
    private String score;
    @QParamDoc(name = "value", description = "The value the score should be larger or equal to.", def = "1")
    private int value;
    @QParamDoc(name = "mode", description = "The mode of the condition. Can be 'at_least' or 'at_most', 'exactly' or 'unset'.", def = "at_least")
    private ScoreConditionMode mode;

    private int lastScoreValue = 0;

    @Override
    public boolean checkInternal(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return fail(quester);
        }
        lastScoreValue = player.getScore(score);
        switch (mode) {
            case AT_LEAST:
                if (lastScoreValue >= value) return success(quester);
                break;
            case AT_MOST:
                if (lastScoreValue <= value) return success(quester);
                break;
            case EXACTLY:
                if (lastScoreValue == value) return success(quester);
                break;
            case UNSET:
                if (lastScoreValue == 0) return success(quester);
                break;
        }
        return fail(player);
    }

    /** Exposes %score_value% to child actions (onSuccess / onFail). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("score_value", new QVariable(lastScoreValue));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        score = cfg.getString("score");
        value = cfg.getInt("value");
        mode = ScoreConditionMode.valueOf(cfg.getString("mode", "at_least").toUpperCase());
    }
}
