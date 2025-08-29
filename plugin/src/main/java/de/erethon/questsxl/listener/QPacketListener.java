package de.erethon.questsxl.listener;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.data.QDatabaseManager;
import de.erethon.questsxl.player.QPlayer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;

public class QPacketListener extends ChannelDuplexHandler {

    QuestsXL plugin;
    ServerPlayer player;
    QDatabaseManager databaseManager;

    public QPacketListener(QuestsXL plugin, ServerPlayer player) {
        this.plugin = plugin;
        this.player = player;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ClientboundSystemChatPacket chatPacket) {
            QPlayer qPlayer = databaseManager.getCurrentPlayer(player.getBukkitEntity());
            if (qPlayer == null) {
                super.write(ctx, msg, promise);
                return;
            }
            Component component;
            if (chatPacket.content() != null) {
                component = PaperAdventure.asAdventure(chatPacket.content());
            } else {
                QuestsXL.log("Adventure & json content in chat packet for " + player + " are both null!");
                return;
            }

            if (component.contains(Component.text("").clickEvent(ClickEvent.runCommand("qxl_marker")), Component.EQUALS)) {
                super.write(ctx, msg, promise); // Redirect marked messages to the player without saving them
                return;
            } else {
                qPlayer.addChat(component); // Add non-marked messages to the backlog
            }

        }
        super.write(ctx, msg, promise);
    }



}
