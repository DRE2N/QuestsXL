package de.erethon.questsxl.listener;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.event.QRegionEnterEvent;
import de.erethon.questsxl.event.QRegionLeaveEvent;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.region.QRegion;
import de.erethon.questsxl.region.QRegionManager;
import de.erethon.questsxl.region.RegionFlag;
import de.erethon.questsxl.respawn.RespawnPoint;
import io.netty.channel.ChannelPipeline;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener extends AbstractListener {

    QuestsXL plugin = QuestsXL.get();
    QRegionManager regionManager = plugin.getRegionManager();

    // Store death locations temporarily for respawn calculation
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    @EventHandler
    public void onLogin(PlayerJoinEvent event) {
        CraftPlayer bukkitPlayer = (CraftPlayer) event.getPlayer();
        ServerPlayer serverPlayer = bukkitPlayer.getHandle();
        QPacketListener packetHandler = new QPacketListener(plugin, serverPlayer);
        ChannelPipeline pipeline = serverPlayer.connection.connection.channel.pipeline();
        pipeline.addAfter("packet_handler", "qxl_handler", packetHandler); // Server -> QXL -> Client
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        QPlayer player = databaseManager.getCurrentPlayer(event.getPlayer());
        if (player == null) {
            return; // Player likely in char selection
        }
        for (QEvent qEvent : plugin.getEventManager().getEvents()) {
            qEvent.removePlayerOnDisconnect(player);
        }

        // Clean up stored death location to prevent memory leaks
        deathLocations.remove(event.getPlayer().getUniqueId());
        player.saveToDatabase();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom() == event.getTo()) {
            return;
        }
        Player player = event.getPlayer();
        QPlayer qp = databaseManager.getCurrentPlayer(player);
        if (qp == null) {
            return; // Player likely in char selection
        }
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

            // Trigger automatic tracking update when player moves to a new chunk
            if (plugin.getAutoTrackingManager() != null) {
                plugin.getAutoTrackingManager().triggerImmediateUpdate(qp);
            }
        }

        // Check for proximity-based explorable discoveries on every move
        qp.getExplorer().checkProximityDiscoveries();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        QPlayer player = databaseManager.getCurrentPlayer(event.getPlayer());
        if (player == null) {
            return; // Player likely in char selection
        }
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
        QPlayer qplayer = databaseManager.getCurrentPlayer(player);
        if (qplayer == null) {
            return region.hasPublicFlag(flag);
        }
        if (qplayer.hasQuest(region.getLinkedQuest())) {
            return region.hasQuestFlag(flag);
        }
        return region.hasPublicFlag(flag);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        QPlayer qPlayer = databaseManager.getCurrentPlayer(event.getPlayer());
        if (qPlayer == null) {
            return; // Player likely in char selection
        }

        deathLocations.put(event.getPlayer().getUniqueId(), event.getPlayer().getLocation().clone());

        for (QEvent qEvent : plugin.getEventManager().getEvents()) {
            qEvent.removePlayerOnDisconnect(qPlayer);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        QPlayer qPlayer = databaseManager.getCurrentPlayer(event.getPlayer());
        if (qPlayer == null) {
            return; // Player likely in char selection
        }

        Location deathLocation = deathLocations.remove(event.getPlayer().getUniqueId());
        if (deathLocation == null) {
            deathLocation = event.getPlayer().getLocation();
        }

        RespawnPoint bestRespawnPoint = plugin.getRespawnPointManager().getBestRespawnPoint(qPlayer, deathLocation);

        if (bestRespawnPoint != null) {
            event.setRespawnLocation(bestRespawnPoint.getLocation());

            qPlayer.getExplorer().setLastRespawnPoint(bestRespawnPoint.getId());
            bestRespawnPoint.setLastUsed();

            qPlayer.sendMessage(Component.translatable("qxl.respawn.location",
                bestRespawnPoint.getDisplayName().get()));
        }
    }
}
