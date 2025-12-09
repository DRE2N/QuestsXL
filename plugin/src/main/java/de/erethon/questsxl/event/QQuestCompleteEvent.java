package de.erethon.questsxl.event;

import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class QQuestCompleteEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final QPlayer qPlayer;
    private final QQuest qQuest;

    public QQuestCompleteEvent(Player p, QPlayer qPlayer, QQuest qQuest) {
        this.player = p;
        this.qPlayer = qPlayer;
        this.qQuest = qQuest;
    }

    public  Player getPlayer() {
        return player;
    }

    public ObjectiveHolder getQPlayer() {
        return qPlayer;
    }

    public QQuest getQuest() {
        return qQuest;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
