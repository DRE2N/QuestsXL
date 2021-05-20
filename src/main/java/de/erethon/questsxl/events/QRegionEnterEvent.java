package de.erethon.questsxl.events;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.regions.QRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class QRegionEnterEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    QRegion region;
    Player player;
    public QRegionEnterEvent(Player player, QRegion region) {
        this.region = region;
        this.player = player;
        MessageUtil.log(player.getName() + " entered region " + region.getId());
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
