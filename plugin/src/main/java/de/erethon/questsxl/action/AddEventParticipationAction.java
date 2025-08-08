package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "event_participation",
        description = "Add participation to an event.",
        shortExample = "event_participation: amount=1",
        longExample = {
                "event_participation:",
                "  id: example_event",
                "  amount: 1"
        }
)
public class AddEventParticipationAction extends QBaseAction {

    QuestsXL plugin = QuestsXL.get();

    @QParamDoc(name = "id", description = "The ID of the event to participate in. Defaults to the top parent event if not specified.")
    private String id = null;
    @QParamDoc(name = "amount", description = "The amount of participation to add", def="1")
    private int amount;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        QEvent event;
        if (id != null) {
            event = plugin.getEventManager().getByID(id);
        } else if (findTopParent() instanceof QEvent e) {
            event = e;
        } else {
            throw new IllegalArgumentException("No event ID specified and no parent event found.");
        }
        execute(quester, (QPlayer player) -> event.participate(player, amount));
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        id = cfg.getString("id", null);
        amount = cfg.getInt("amount", 1);
    }

}
