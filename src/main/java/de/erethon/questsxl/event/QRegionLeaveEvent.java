package de.erethon.questsxl.event;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.region.QRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class QRegionLeaveEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    QRegion region;
    Player player;
    public QRegionLeaveEvent(Player player, QRegion region) {
        this.region = region;
        this.player = player;
        MessageUtil.log(player.getName() + " left region " + region.getId());
    }

    public QRegion getRegion() {
        return region;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
