package de.erethon.questsxl.commands;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Date;

public class MainCommand extends DRECommand {


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
        if (player.hasPermission("qxl.admin.version")) {
            MessageUtil.sendMessage(player, "&8&m   &r &aQuests&2XL &6" + plugin.getDescription().getVersion() + " &7by Malfrador &8&m   &r");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "&7Last sync from GitHub: &6" + new Date(plugin.lastSync));
            File[] playerFiles = QuestsXL.PLAYERS.listFiles();
            if (playerFiles != null) {
                MessageUtil.sendMessage(player, "&7Players: &6" + playerFiles.length);
            }
            MessageUtil.sendMessage(player, "&7Quests: &6" + plugin.getQuestManager().getQuests().size());
            MessageUtil.sendMessage(player, "&7Regions: &6" + plugin.getRegionManager().getRegions().size());
            MessageUtil.sendMessage(player, "&7IBCs: &6" + plugin.getBlockCollectionManager().getCollections().size());
            MessageUtil.sendMessage(player, "&7Global Objectives: &6" + 0);
            return;
        }
        if (args.length >= 2) {
            player.performCommand("qxl q " + args[1]);
            return;
        }
        player.performCommand("qxl q");
    }
}
