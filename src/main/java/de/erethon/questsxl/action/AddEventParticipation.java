package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import org.bukkit.configuration.ConfigurationSection;

public class AddEventParticipation extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QPlayerCache playerCache = plugin.getPlayerCache();

    private QEvent event;
    private int amount;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        event.participate(player, amount);
        onFinish(player);
    }

    @Override
    public void onFinish(QEvent event) {

    }

    @Override
    public void load(String[] msg) {
        event = plugin.getEventManager().getByID(msg[0]);
        amount = Integer.parseInt(msg[1]);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        event =  plugin.getEventManager().getByID(section.getString("message"));
        amount = section.getInt("amount");
    }
}
