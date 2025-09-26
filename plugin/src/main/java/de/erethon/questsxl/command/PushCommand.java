package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.command.CommandSender;

public class PushCommand extends ECommand {

    public PushCommand() {
        setCommand("push");
        setMinArgs(0);
        setMaxArgs(1);
        setPlayerCommand(true);
        setConsoleCommand(true);
        setHelp("Push server-modified files to GitHub repository. Usage: /q push [force]");
        setPermission("qxl.admin.push");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        boolean force = args.length > 0 && args[0].equalsIgnoreCase("force");

        if (force) {
            MessageUtil.sendMessage(commandSender, "&cForce pushing server changes to GitHub...");
            MessageUtil.sendMessage(commandSender, "&e&lWARNING: This will overwrite remote changes!");
        } else {
            MessageUtil.sendMessage(commandSender, "&aPushing server changes to GitHub...");
        }
        MessageUtil.sendMessage(commandSender, "&7This may take a moment.");
        QuestsXL.get().pushServerChanges(force);
    }
}
