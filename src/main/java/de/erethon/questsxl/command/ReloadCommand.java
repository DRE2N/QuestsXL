package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadCommand extends ECommand {


    public ReloadCommand() {
        setCommand("reload");
        setMinArgs(0);
        setMaxArgs(1);
        setPlayerCommand(true);
        setConsoleCommand(true);
        setHelp("Help.");
        setPermission("qxl.admin.reload");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        if (args.length > 1) {
            QuestsXL.getInstance().setShowStacktraces(true);
        }
        MessageUtil.sendMessage(commandSender, "&aReloading...");
        QuestsXL.getInstance().reload();
        MessageUtil.sendMessage(commandSender, "&aReload complete.");
        List<FriendlyError> errors = QuestsXL.getInstance().getErrors();
        if (!errors.isEmpty()) {
            MessageUtil.sendMessage(commandSender, "&4Es sind &c" + errors.size() + " &4Fehler aufgetreten:");
        }
        for (FriendlyError error : errors) {
            MessageUtil.sendMessage(commandSender, error.getMessage());
        }
        QuestsXL.getInstance().setShowStacktraces(false);
    }
}