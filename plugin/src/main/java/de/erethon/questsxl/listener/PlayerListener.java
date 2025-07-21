package de.erethon.questsxl.listener;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.event.QRegionEnterEvent;
import de.erethon.questsxl.event.QRegionLeaveEvent;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.region.QRegion;
import de.erethon.questsxl.region.QRegionManager;
import de.erethon.questsxl.region.RegionFlag;
import io.netty.channel.ChannelPipeline;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener extends AbstractListener {

    QuestsXL plugin = QuestsXL.getInstance();
    QRegionManager regionManager = plugin.getRegionManager();

    @EventHandler
    public void onLogin(PlayerJoinEvent event) {
        CraftPlayer bukkitPlayer = (CraftPlayer) event.getPlayer();
        ServerPlayer serverPlayer = bukkitPlayer.getHandle();
        QPacketListener packetHandler = new QPacketListener(plugin, serverPlayer);
        ChannelPipeline pipeline = serverPlayer.connection.connection.channel.pipeline();
        //pipeline.addAfter("packet_handler", "qxl_handler", packetHandler); // Server -> QXL -> Client
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        QPlayer player = cache.getByPlayer(event.getPlayer());
        for (QEvent qEvent : plugin.getEventManager().getEvents()) {
            qEvent.removePlayerOnDisconnect(player);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom() == event.getTo()) {
            return;
        }
        Player player = event.getPlayer();
        QPlayer qp = cache.getByPlayer(player);
        if (qp.isFrozen()) {
            event.setCancelled(true);
            return;
        }
        QRegion regionFrom = regionManager.getByLocation(event.getFrom());
        QRegion regionTo = regionManager.getByLocation(event.getTo());
        if (regionFrom != null && regionTo == null) {
            Bukkit.getPluginManager().callEvent(new QRegionLeaveEvent(player, regionFrom));
            qp.getRegions().remove(regionFrom);
        }
        if (regionFrom == null && regionTo != null) {
            Bukkit.getPluginManager().callEvent(new QRegionEnterEvent(player, regionTo));
            qp.getRegions().add(regionTo);
        }
        if (event.getFrom().getChunk() != event.getTo().getChunk()) {
            qp.getExplorer().updateClosestSet();
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        QPlayer player = cache.getByPlayer(event.getPlayer());
        if (player.isInConversation()) {
            MessageUtil.sendActionBarMessage(player.getPlayer(), QuestsXL.ERROR + "Du kannst den Chat jetzt nicht nutzen.");
            event.setCancelled(true); // TODO: Aergia seems to ignore this
        }
    }


    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        QRegion region = regionManager.getByLocation(event.getBlock().getLocation());
        if (region == null) {
            return;
        }
        event.setCancelled(hasFlag(event.getPlayer(), region, RegionFlag.PROTECTED));
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        QRegion region = regionManager.getByLocation(event.getBlock().getLocation());
        if (region == null) {
            return;
        }
        event.setCancelled(hasFlag(event.getPlayer(), region, RegionFlag.PROTECTED));
    }

    private boolean hasFlag(Player player, QRegion region, RegionFlag flag) {
        if (region.getLinkedQuest() == null) {
            return region.hasPublicFlag(flag);
        }
        QPlayer qplayer = cache.getByPlayer(player);
        if (qplayer.hasQuest(region.getLinkedQuest())) {
            return region.hasQuestFlag(flag);
        }
        return region.hasPublicFlag(flag);
    }

}
