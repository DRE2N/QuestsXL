package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.command.CommandSender;

public class SyncCommand extends ECommand {

    public SyncCommand() {
        setCommand("sync");
        setMinArgs(0);
        setMaxArgs(1);
        setPlayerCommand(true);
        setConsoleCommand(true);
        setHelp("Synchronize data with GitHub. Usage: /q sync [full|push|pull] (defaults to pull)");
        setPermission("qxl.admin.sync");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        if (args.length == 1) {
            MessageUtil.sendMessage(commandSender, "&aPulling updates from GitHub...");
            MessageUtil.sendMessage(commandSender, "&7Server changes will be backed up before pulling.");
            MessageUtil.sendMessage(commandSender, "&7This may take a moment.");
            QuestsXL.get().fullSync();
        } else {
            String syncType = args[1].toLowerCase();
            switch (syncType) {
                case "full":
                    MessageUtil.sendMessage(commandSender, "&aSynchronizing with GitHub (full sync)...");
                    MessageUtil.sendMessage(commandSender, "&7This will push server changes first, then pull updates.");
                    MessageUtil.sendMessage(commandSender, "&7This may take a moment.");
                    QuestsXL.get().fullSync();
                    break;
                case "push":
                    MessageUtil.sendMessage(commandSender, "&aPushing server changes to GitHub...");
                    MessageUtil.sendMessage(commandSender, "&7Only server-modified files will be pushed.");
                    MessageUtil.sendMessage(commandSender, "&7This may take a moment.");
                    QuestsXL.get().pushServerChanges();
                    break;
                case "pull":
                    MessageUtil.sendMessage(commandSender, "&aPulling updates from GitHub...");
                    MessageUtil.sendMessage(commandSender, "&7Server changes will be backed up before pulling.");
                    MessageUtil.sendMessage(commandSender, "&7This may take a moment.");
                    QuestsXL.get().sync();
                    break;
                default:
                    MessageUtil.sendMessage(commandSender, "&cInvalid sync type. Use: full, push, or pull");
                    return;
            }
        }
    }
}