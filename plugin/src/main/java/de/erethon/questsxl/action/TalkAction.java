package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedHashMap;
import java.util.Map;

public class TalkAction extends QBaseAction {

    Map<String, Integer> messages = new LinkedHashMap<>();

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        if (!(quester instanceof QPlayer player)) {
            return;
        }
        messages.put("Hallo", 2);
        messages.put("Hallo zwei", 1);
        messages.put("Hallo zwei diese Nachricht wird etwas dauern.", 10);
        messages.put("Hallo jetzt dauerts nicht mehr so lang", 5);
        player.setInConversation(true);
        int time = 0;
        int numMessage = 0;
        int total = messages.size();
        for (String msg : messages.keySet()) {
            numMessage++;
            int finalNumMessage = numMessage;
            BukkitRunnable later = new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendConversationMsg("<dark_gray>[<gray>" + finalNumMessage + "<dark_gray>/<gray>" + total + "<dark_gray>] <green>" + msg, "", 1, 1);
                    messages.remove(msg);
                    if (messages.isEmpty()) {
                        player.setInConversation(false);
                        player.sendMessagesInQueue(false);
                        onFinish(player);
                    }
                }
            };
            time = time + (messages.get(msg) * 20);
            later.runTaskLater(QuestsXL.getInstance(), time);
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
    }
}
