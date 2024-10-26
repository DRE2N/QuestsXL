package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NPCCommand extends ECommand {

    QuestsXL plugin = QuestsXL.getInstance();

    public NPCCommand() {
        setCommand("npc");
        setAliases("n");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.admin.npc");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (player.getTargetBlockExact(10) == null) {
            MessageUtil.sendMessage(player, "&cNo target.");
            return;
        }
        MessageUtil.sendMessage(player, "&aSpawned NPC named " + args[1]);
    }
}
