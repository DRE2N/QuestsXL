package de.erethon.questsxl.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.player.QPlayerCache;
import de.erethon.questsxl.tool.packetwrapper.WrapperPlayServerChat;

public class PacketListener {

    ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
    QuestsXL plugin = QuestsXL.getInstance();
    QPlayerCache cache = QuestsXL.getInstance().getPlayerCache();


    public PacketListener() {
        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                onChat(event);
            }
        });
    }

    private void onChat(PacketEvent event) {
        WrapperPlayServerChat packet = new WrapperPlayServerChat(event.getPacket());
        if (packet.getChatType() == EnumWrappers.ChatType.GAME_INFO) {
            return;
        }
        QPlayer qPlayer = cache.get(event.getPlayer());
        if (qPlayer != null && qPlayer.isInConversation()) {
            event.setCancelled(true);
            qPlayer.addChat(packet.getMessage());
        }
    }
}
