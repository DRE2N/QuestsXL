package de.erethon.questsxl.event;

import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class QEventCompleteEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final QPlayer qPlayer;
    private final QEvent qEvent;
    private final int participation;

    public QEventCompleteEvent(Player p, QPlayer qPlayer, QEvent qEvent, int participation) {
        this.player = p;
        this.qPlayer = qPlayer;
        this.qEvent = qEvent;
        this.participation = participation;
    }

    public  Player getPlayer() {
        return player;
    }

    public QPlayer getQPlayer() {
        return qPlayer;
    }

    public QEvent getQEvent() {
        return qEvent;
    }

    public int getParticipation() {
        return participation;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
