package de.erethon.questsxl.commands;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.players.QPlayer;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.quest.QStage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand extends DRECommand {

    QuestsXL plugin = QuestsXL.getInstance();

    public AdminCommand() {
        setCommand("admin");
        setAliases("a");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.admin.admincommand");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args.length > 1 && (args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("i"))) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe eine Quest an.");
                return;
            }
            QQuest quest = plugin.getQuestManager().getByName(args[2]);
            if (quest == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Quest existiert nicht.");
                return;
            }
            MessageUtil.sendMessage(player, "&7ID: &6" + quest.getName());
            MessageUtil.sendMessage(player, "&7Name: &6" + quest.getDisplayName());
            MessageUtil.sendMessage(player, "&7Description: &6" + quest.getDescription());
            MessageUtil.sendMessage(player, "&7Stages (" + quest.getStages().size() + "):");
            for (QStage stage : quest.getStages()) {
                MessageUtil.sendMessage(player, "&8- [&6" + stage.getId() + "&8] &7" + stage.getDescription());
            }
            return;
        }
        if (args.length > 1 && (args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase("ls"))) {
            if (args.length < 3) {
                for (QQuest quest : plugin.getQuestManager().getQuests()) {
                    MessageUtil.sendMessage(player, "&8- " + quest.getName());
                }
                return;
            }
            Player otherPlayer = Bukkit.getPlayer(args[2]);
            if (otherPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Dieser Spieler existiert nicht");
                return;
            }
            QPlayer qPlayer = plugin.getPlayerCache().get(otherPlayer);
            if (qPlayer.getActiveQuests().isEmpty()) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Dieser hat keine aktiven Quests.");
                return;
            }
            for (ActiveQuest active : qPlayer.getActiveQuests().keySet()) {
                MessageUtil.sendMessage(player, "&8- &6" + active.getQuest().getName() + ": &8[&b" + active.getCurrentStage().getId() + "&8] " + active.getCurrentStage().getDescription());
            }
            return;
        }
        MessageUtil.sendMessage(player, QuestsXL.ERROR + "Unbekannter Befehl. Probiere z.B: /q admin info oder /q admin list");
    }
}