package de.erethon.questsxl.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CommandTriggerEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    String id;
    Player player;
    public CommandTriggerEvent(Player player, String id) {
        this.id = id;
        this.player = player;
    }

    public String getID() {
        return id;
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
