package de.erethon.questsxl.event;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.region.QRegion;
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
        QuestsXL.log(player.getName() + " entered region " + region.getId());
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
