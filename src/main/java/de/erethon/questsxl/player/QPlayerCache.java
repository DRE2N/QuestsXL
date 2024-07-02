package de.erethon.questsxl.player;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.user.UserCache;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class QPlayerCache extends UserCache<QPlayer> {

    public QPlayerCache(QuestsXL plugin) {
        super(plugin);
        setUnloadAfter(0);
        loadAll();
    }

    @Override
    protected QPlayer getNewInstance(@NotNull OfflinePlayer offlinePlayer) {
        Player player = offlinePlayer.getPlayer();
        if (player != null) {
            MessageUtil.log("Loading player " + offlinePlayer.getUniqueId() + " (" + offlinePlayer.getName() + ")...");
            return new QPlayer(player);
        }
        return null;
    }
}
