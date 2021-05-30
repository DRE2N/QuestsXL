package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.players.QPlayer;
import de.erethon.questsxl.players.QPlayerCache;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

enum Scope {
    GLOBAL,
    PLAYER
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
    public void play(Player player) {
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
                QPlayer qp = playerCache.get(player);
                switch (operation) {
                    case ADD -> qp.addScore(score, amount);
                    case REMOVE -> qp.removeScore(score, amount);
                    case SET -> qp.setScore(score, amount);
                    case RESET -> qp.setScore(score, 0);
                }
            }
        }
        onFinish(player);
    }

    @Override
    public void load(String[] msg) {
        score = msg[0];
        amount = Integer.parseInt(msg[1]);
        scope = Scope.valueOf(msg[2].toUpperCase());
        operation = Operation.valueOf(msg[3].toUpperCase());
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        score =  section.getString(score);
        amount = section.getInt("value");
        scope = Scope.valueOf(section.getString("scope", "player").toUpperCase());
        operation = Operation.valueOf(section.getString("operation", "add").toUpperCase());
    }
}
