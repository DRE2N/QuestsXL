package de.erethon.questsxl.commands;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.ActionManager;
import de.erethon.questsxl.action.QAction;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class RunCommand extends DRECommand {

    public RunCommand() {
        setCommand("run");
        setMinArgs(0);
        setMaxArgs(2);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.admin.run");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("input." + args[1], args[2]);
        Set<QAction> qActions = ActionManager.loadActions("cmd", configuration.getConfigurationSection("input"));
        MessageUtil.sendMessage(player, "Loaded " + args[2]);
        BukkitRunnable later = new BukkitRunnable() {
            @Override
            public void run() {
                MessageUtil.sendMessage(player, "Playing " + qActions.size() + " actions...");
                qActions.forEach(action -> action.play(player));
            }
        };
        later.runTaskLater(QuestsXL.getInstance(), 20);
    }
}
