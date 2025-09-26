package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.gui.ExplorationGUI;
import de.erethon.questsxl.player.QPlayer;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Date;

public class ExplorationCommand extends ECommand {

    public ExplorationCommand() {
        setCommand("exploration");
        setMinArgs(0);
        setMaxArgs(0);
        setPlayerCommand(true);
        setHelp("Help.");
        setRegisterSeparately(true);
        setPermission("qxl.user.exploration");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        new ExplorationGUI(player).open();
    }
}
