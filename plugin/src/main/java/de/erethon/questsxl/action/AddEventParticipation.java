package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;

public class AddEventParticipation extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QPlayerCache playerCache = plugin.getPlayerCache();

    private String id;
    private int amount;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        QEvent event = plugin.getEventManager().getByID(id);
        event.participate(player, amount);
        onFinish(player);
    }

    @Override
    public void onFinish(QEvent event) {

    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        id = cfg.getString("id");
        amount = cfg.getInt("amount", 1);
    }

}
