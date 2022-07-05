package de.erethon.questsxl.player;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.dialogue.ActiveDialogue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class QPlayerCache implements Listener {

    QuestsXL plugin = QuestsXL.getInstance();

    private final Map<Player, QPlayer> players = new HashMap<>();
    private final Set<QGroup> groups = new HashSet<>();

    public QPlayerCache() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.put(player, new QPlayer(player));
        }
    }

    public Map<Player, QPlayer> getPlayers() {
        return players;
    }

    public QPlayer get(Player player) {
        return players.get(player);
    }

    public boolean isInGroup(QPlayer player) {
        for (QGroup group : groups) {
            if (group.getMembers().contains(player)) {
                return true;
            }
        }
        return false;
    }

    public QGroup getGroup(QPlayer player) {
        for (QGroup group : groups) {
            if (group.getMembers().contains(player)) {
                return group;
            }
        }
        return null;
    }

    @EventHandler
    public void loginEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (get(player) != null) {
            MessageUtil.log("Player already loaded.");
            return;
        }
        players.put(player, new QPlayer(player));
        MessageUtil.log("Loaded data for " + player.getName());
    }

    @EventHandler
    public void logoffEvent(PlayerQuitEvent event) {
        QPlayer removed = players.remove(event.getPlayer());
        if (removed == null) {
            return;
        }
        ActiveDialogue activeDialogue = removed.getActiveDialogue();
        if (activeDialogue != null) {
            activeDialogue.cancel();
        }
    }

    public static File getFile(UUID uuid) {
        return new File(QuestsXL.PLAYERS, uuid.toString() + ".yml");
    }
}
