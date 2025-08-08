package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.command.CommandSender;

public class SyncCommand extends ECommand {


    public SyncCommand() {
        setCommand("sync");
        setMinArgs(0);
        setMaxArgs(0);
        setPlayerCommand(true);
        setConsoleCommand(true);
        setHelp("Help.");
        setPermission("qxl.admin.sync");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        MessageUtil.sendMessage(commandSender, "&aSynchronisiere Daten mit Github...");
        MessageUtil.sendMessage(commandSender, "&7Dies kann einen Moment dauern.");
        QuestsXL.get().sync();
    }
}