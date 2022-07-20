package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedHashMap;
import java.util.Map;

public class TalkAction extends QBaseAction {

    Map<String, Integer> messages = new LinkedHashMap<>();

    @Override
    public void play(Player player) {
        messages.put("Hallo", 2);
        messages.put("Hallo zwei", 1);
        messages.put("Hallo zwei diese Nachricht wird etwas dauern.", 10);
        messages.put("Hallo jetzt dauerts nicht mehr so lang", 5);
        QPlayer qPlayer = cache.getByPlayer(player);
        qPlayer.setInConversation(true);
        int time = 0;
        int numMessage = 0;
        int total = messages.size();
        for (String msg : messages.keySet()) {
            numMessage++;
            int finalNumMessage = numMessage;
            BukkitRunnable later = new BukkitRunnable() {
                @Override
                public void run() {
                    qPlayer.sendConversationMsg("<dark_gray>[<gray>" + finalNumMessage + "<dark_gray>/<gray>" + total + "<dark_gray>] <green>" + msg);
                    messages.remove(msg);
                    if (messages.isEmpty()) {
                        qPlayer.setInConversation(false);
                        qPlayer.sendMessagesInQueue();
                        onFinish(player);
                    }
                }
            };
            time = time + (messages.get(msg) * 20);
            later.runTaskLater(QuestsXL.getInstance(), time);
        }
    }

    @Override
    public void onFinish(Player player) {
        super.onFinish(player);
    }

    @Override
    public void load(String[] msg) {
        super.load(msg);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
    }
}
