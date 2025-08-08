package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;

enum Scope {
    GLOBAL,
    PLAYER,
    EVENT
}

enum Operation {
    ADD,
    REMOVE,
    SET,
    RESET
}

@QLoadableDoc(
        value = "score",
        description = """
                Modifies a score value. The score can be global, player-specific or event-specific.\s
                Scores are a powerful tool to track player progress and can be used in conditions and actions.\
                
                For example, you could add 1 to a score called enemy_threat every time a player kills a mob during an event, and if the score reaches a certain value, you could spawn a boss.""",
        shortExample = "score: score=enemy_threat; value=1; operation=add scope=event",
        longExample = {
                "score:",
                "  score: enemy_threat",
                "  value: 1",
                "  operation: add",
                "  scope: event"
        }
)
public class ScoreAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.get();
    QPlayerCache playerCache = plugin.getPlayerCache();

    @QParamDoc(name = "score", description = "The score to modify", required = true)
    private String score;
    @QParamDoc(name = "value", description = "The value to add, remove, set or reset", def = "1")
    private int amount;
    @QParamDoc(name = "scope", description = "The scope of the score. One of `global`, `player` or `event`", def = "`player`")
    private Scope scope;
    @QParamDoc(name = "operation", description = "The operation to perform. One of `add`, `remove`, `set` or `reset`", def = "`add`")
    private Operation operation;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        switch (scope) {
            case GLOBAL -> {
                switch (operation) {
                    case ADD -> plugin.addScore(score, amount);
                    case REMOVE -> plugin.removeScore(score, amount);
                    case SET -> plugin.setScore(score, amount);
                    case RESET -> plugin.setScore(score, 0);
                }
            }
            case PLAYER -> {
                execute(quester, this::modifyScore);
            }
        }
        onFinish(quester);
    }

    private void modifyScore(QPlayer player) {
        switch (operation) {
            case ADD -> player.addScore(score, amount);
            case REMOVE -> player.removeScore(score, amount);
            case SET -> player.setScore(score, amount);
            case RESET -> player.setScore(score, 0);
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        score =  cfg.getString("score");
        amount = cfg.getInt("value", 1);
        scope = Scope.valueOf(cfg.getString("scope", "player").toUpperCase());
        operation = Operation.valueOf(cfg.getString("operation", "add").toUpperCase());
    }
}
