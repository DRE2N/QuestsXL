package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import org.bukkit.configuration.ConfigurationSection;

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

public class ScoreAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QPlayerCache playerCache = plugin.getPlayerCache();

    private String score;
    private int amount;
    private Scope scope;
    private Operation operation;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
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
                switch (operation) {
                    case ADD -> player.addScore(score, amount);
                    case REMOVE -> player.removeScore(score, amount);
                    case SET -> player.setScore(score, amount);
                    case RESET -> player.setScore(score, 0);
                }
            }
        }
        onFinish(player);
    }

    @Override
    public void play(QEvent event) {
        if (!conditions(event)) return;
        switch (scope) {
            case GLOBAL -> {
                switch (operation) {
                    case ADD -> plugin.addScore(score, amount);
                    case REMOVE -> plugin.removeScore(score, amount);
                    case SET -> plugin.setScore(score, amount);
                    case RESET -> plugin.setScore(score, 0);
                }
            }
            case EVENT -> {
                switch (operation) {
                    case ADD -> event.addScore(score, amount);
                    case REMOVE -> event.removeScore(score, amount);
                    case SET -> event.setScore(score, amount);
                    case RESET -> event.setScore(score, 0);
                }
            }
        }
        onFinish(event);
    }

    @Override
    public void load(QLineConfig cfg) {
        score = cfg.getString("score");
        amount = cfg.getInt("value");
        scope = Scope.valueOf(cfg.getString("scope", "player").toUpperCase());
        operation = Operation.valueOf(cfg.getString("operation", "add").toUpperCase());
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        score =  section.getString("score");
        amount = section.getInt("value");
        scope = Scope.valueOf(section.getString("scope", "player").toUpperCase());
        operation = Operation.valueOf(section.getString("operation", "add").toUpperCase());
    }
}
