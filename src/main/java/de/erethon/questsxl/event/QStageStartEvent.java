package de.erethon.questsxl.event;

import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.common.QStage;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class QStageStartEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    QStage stage;
    QPlayer player;
    public QStageStartEvent(QStage stage, QPlayer player) {
        this.stage = stage;
        this.player = player;
    }

    public QStage getStage() {
        return stage;
    }

    public QPlayer getPlayer() {
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
