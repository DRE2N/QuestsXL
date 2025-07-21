package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Date;

public class MainCommand extends ECommand {

    public MainCommand() {
        setCommand("main");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.user.main");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        QuestsXL plugin = QuestsXL.getInstance();
        Player player = (Player) commandSender;
        QPlayer qPlayer = plugin.getPlayerCache().getByPlayer(player);
        if (player.hasPermission("qxl.admin.version")) {
            MessageUtil.sendMessage(player, "&8&m   &r &aQuests&2XL &6" + plugin.getDescription().getVersion() + " &7by Malfrador &8&m   &r");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "&7Internals: &6" + MinecraftServer.getServer().getServerModName()
                    + " " + MinecraftServer.getServer().getServerVersion());
            MessageUtil.sendMessage(player, "&7Last sync from GitHub: &6" + new Date(plugin.lastSync));
            File[] playerFiles = QuestsXL.PLAYERS.listFiles();
            if (playerFiles != null) {
                MessageUtil.sendMessage(player, "&7Players: &6" + playerFiles.length + " &8- &7Loaded: &6"
                        + plugin.getPlayerCache().getCachedUsersAmount());
            }
            MessageUtil.sendMessage(player, "&7Quests: &6" + plugin.getQuestManager().getQuests().size()
                    + "&8 - &7Global Objectives: &6" + plugin.getGlobalObjectives().getObjectives().size());
            MessageUtil.sendMessage(player, "&7Regions: &6" + plugin.getRegionManager().getRegions().size()
                    + "&8 - &7IBCs: &6" + plugin.getBlockCollectionManager().getCollections().size());
            MessageUtil.sendMessage(player, "&7Cutscenes: &6" + plugin.getAnimationManager().getCutscenes().size()
                    + " &8- &7Animations: &6" + plugin.getAnimationManager().getAnimations().size());
            MessageUtil.sendMessage(player,"&7Events: &6" + plugin.getEventManager().getEvents().size()
                    + " &8- &7Aktiv: &6" + plugin.getEventManager().getActiveEvents().size());
            if (qPlayer.getTrackedEvent() != null) {
                String trackedEventObjective = qPlayer.getTrackedEvent().getObjectiveDisplayText();
                if (trackedEventObjective != null) {
                    MessageUtil.sendMessage(player, "&7Tracked Event: &6" + qPlayer.getTrackedEvent().getName()
                            + " &8- &7Objective: &6" + trackedEventObjective);
                } else {
                    MessageUtil.sendMessage(player, "&7Tracked Event: &6" + qPlayer.getTrackedEvent().getName());
                }
            } else {
                MessageUtil.sendMessage(player, "&7No tracked event.");
            }
            return;
        }
        if (args.length >= 2) {
            player.performCommand("qxl q " + args[1]);
            return;
        }
        player.performCommand("qxl q");
    }
}
