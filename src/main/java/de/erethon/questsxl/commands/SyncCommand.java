package de.erethon.questsxl.commands;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.command.CommandSender;

public class SyncCommand extends DRECommand {


    public SyncCommand() {
        setCommand("sync");
        setMinArgs(0);
        setMaxArgs(0);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.admin.sync");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        MessageUtil.sendMessage(commandSender, "&aSynchronisiere Daten mit Github...");
        MessageUtil.sendMessage(commandSender, "&7Dies kann einen Moment dauern.");
        QuestsXL.getInstance().sync();
    }
}