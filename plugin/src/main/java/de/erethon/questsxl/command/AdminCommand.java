package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.common.QStage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand extends ECommand {

    QuestsXL plugin = QuestsXL.get();

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
        if (args.length < 2) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Unbekannter Befehl. Probiere z.B: /q admin info oder /q admin list");
            return;
        }
        if (args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("i")) {
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
        if (args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase("ls")) {
            if (args.length < 3) {
                for (QQuest quest : plugin.getQuestManager().getQuests()) {
                    MessageUtil.sendMessage(player, "&8- &6" + quest.getName());
                }
                return;
            }
            Player otherPlayer = Bukkit.getPlayer(args[2]);
            if (otherPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Dieser Spieler existiert nicht");
                return;
            }
            QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(otherPlayer);
            if (qPlayer.getActiveQuests().isEmpty()) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Dieser hat keine aktiven Quests.");
                return;
            }
            for (ActiveQuest active : qPlayer.getActiveQuests().keySet()) {
                MessageUtil.sendMessage(player, "&8- &6" + active.getQuest().getName() + ": &8[&b" + active.getCurrentStage().getId() + "&8] " + active.getCurrentStage().getDescription());
            }
            return;
        }
        if (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("g")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe einen Spieler an.");
                return;
            }
            Player otherPlayer = Bukkit.getPlayer(args[2]);
            if (otherPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Dieser Spieler existiert nicht");
                return;
            }
            QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(otherPlayer);
            if (args.length < 4) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe eine Quest an.");
                return;
            }
            QQuest quest = plugin.getQuestManager().getByName(args[3]);
            if (quest == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Quest existiert nicht.");
                return;
            }
            qPlayer.addActive(quest);
            MessageUtil.sendMessage(player, "&a" + player.getName() + " &7hat erfolgreich die Quest &a" + quest.getName() + " &7gestartet.");
            return;
        }
        if (args[1].equalsIgnoreCase("objectives") || args[1].equalsIgnoreCase("o")) {
            QPlayer player1 = plugin.getDatabaseManager().getCurrentPlayer(player);
            MessageUtil.sendMessage(player, "&7Aktive Objectives:");
            player1.getCurrentObjectives().forEach((objective) -> {
                MessageUtil.sendMessage(player, "&8- &6" + objective.getObjective().getClass().toString());
            });
            return;
        }
        MessageUtil.sendMessage(player, QuestsXL.ERROR + "Unbekannter Befehl. Probiere z.B: /q admin info, /q admin objectives, /q admin list oder /q admin give");
    }
}