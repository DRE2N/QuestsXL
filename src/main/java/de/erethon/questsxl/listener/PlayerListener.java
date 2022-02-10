package de.erethon.questsxl.listener;

import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.InstancedCreatureDeathEvent;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.event.QRegionEnterEvent;
import de.erethon.questsxl.event.QRegionLeaveEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.region.QRegion;
import de.erethon.questsxl.region.QRegionManager;
import de.erethon.questsxl.region.RegionFlag;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerListener extends AbstractListener {

    QRegionManager manager = QuestsXL.getInstance().getRegionManager();

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom() == event.getTo()) {
            return;
        }
        Player player = event.getPlayer();
        QPlayer qp = cache.get(player);
        if (qp.isFrozen()) {
            event.setCancelled(true);
            return;
        }
        // Objectives
        for (ActiveObjective objective : qp.getCurrentObjectives()) {
            objective.check(event);
        }
        // Regions
        QRegion regionFrom = manager.getByLocation(event.getFrom());
        QRegion regionTo = manager.getByLocation(event.getTo());
        if (regionFrom != null && regionTo == null) {
            Bukkit.getPluginManager().callEvent(new QRegionLeaveEvent(player, regionFrom));
            qp.getRegions().remove(regionFrom);
        }
        if (regionFrom == null && regionTo != null) {
            Bukkit.getPluginManager().callEvent(new QRegionEnterEvent(player, regionTo));
            qp.getRegions().add(regionTo);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        checkObjectives(event.getPlayer(), event);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        QPlayer player = cache.get(event.getPlayer());
        if (player.isInConversation()) {
            player.sendConversationMsg(QuestsXL.ERROR + "Du kannst den Chat jetzt nicht nutzen.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRegionEnter(QRegionEnterEvent event) {
        checkObjectives(event.getPlayer(), event);
    }
    @EventHandler
    public void onRegionLeave(QRegionLeaveEvent event) {
        checkObjectives(event.getPlayer(), event);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        checkObjectives((Player) event.getWhoClicked(), event);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        checkObjectives(event.getPlayer(), event);
        Player killer = event.getPlayer().getKiller();
        if (killer != null) {
            checkObjectives(killer, event);
        }
    }

    @EventHandler
    public void onCreatureDeath(CreatureDeathEvent event) {
        Player killer = event.getKiller();
        if (killer != null) {
            checkObjectives(killer, event);
        }
    }

    @EventHandler
    public void onInstancedCreatureDeath(InstancedCreatureDeathEvent event) {
        Player killer = event.getKiller();
        if (killer != null) {
            checkObjectives(killer, event);
        }
    }


    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        QRegion region = manager.getByLocation(event.getBlock().getLocation());
        if (region == null) {
            return;
        }
        event.setCancelled(hasFlag(event.getPlayer(), region, RegionFlag.PROTECTED));
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        QRegion region = manager.getByLocation(event.getBlock().getLocation());
        if (region == null) {
            return;
        }
        event.setCancelled(hasFlag(event.getPlayer(), region, RegionFlag.PROTECTED));
    }

    private boolean hasFlag(Player player, QRegion region, RegionFlag flag) {
        if (region.getLinkedQuest() == null) {
            return region.hasPublicFlag(flag);
        }
        QPlayer qplayer = cache.get(player);
        if (qplayer.hasQuest(region.getLinkedQuest())) {
            return region.hasQuestFlag(flag);
        }
        return region.hasPublicFlag(flag);
    }

}
