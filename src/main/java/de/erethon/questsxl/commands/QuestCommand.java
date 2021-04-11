package de.erethon.questsxl.commands;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestCommand extends DRECommand {

    QuestsXL plugin = QuestsXL.getInstance();

    public QuestCommand() {
        setCommand("quest");
        setAliases("q");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.quest.create");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args[1].equalsIgnoreCase("give")) {
            QQuest quest = plugin.getQuestManager().getByName(args[2]);
            if (quest == null) {
                MessageUtil.sendMessage(player, "Quest does not exist.");
                return;
            }
            MessageUtil.sendMessage(player, "Starting quest " + quest.getName());
            plugin.getPlayerCache().get(player).startQuest(quest);
        }
    }
}