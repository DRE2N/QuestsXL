package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;

@QLoadableDoc(
        value = "event_participation",
        description = "Add participation to an event. Always executed as a player",
        shortExample = "event_participation: id=example_event; amount=1",
        longExample = {
                "event_participation:",
                "  id: example_event",
                "  amount: 1"
        }
)
public class AddEventParticipationAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.getInstance();
    QPlayerCache playerCache = plugin.getPlayerCache();

    @QParamDoc(name = "id", description = "The ID of the event to participate in", required = true)
    private String id;
    @QParamDoc(name = "amount", description = "The amount of participation to add", def="1")
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
