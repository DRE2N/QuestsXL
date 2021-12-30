package de.erethon.questsxl.commands;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.ActionManager;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RunCommand extends DRECommand {

    List<FriendlyError> errors = new ArrayList<>();

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
        errors.clear();
        Set<QAction> qActions = new HashSet<>();
        try {
            qActions = ActionManager.loadActions("cmd", configuration.getConfigurationSection("input"));
        } catch (Exception exception) {
            errors.add(new FriendlyError("Command", "Failed to parse action", exception.getMessage(), "").addStacktrace(exception.getStackTrace()));
        }
        if (qActions.isEmpty()) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Action konnte nicht geladen werden:");
            for (FriendlyError error : errors) {
                MessageUtil.sendMessage(player, error.getMessage());
            }
            errors.clear();
            return;
        }
        MessageUtil.sendMessage(player, "Loaded " + args[2]);
        Set<QAction> finalQActions = qActions;
        BukkitRunnable later = new BukkitRunnable() {
            @Override
            public void run() {
                MessageUtil.sendMessage(player, "Playing " + finalQActions.size() + " actions...");
                try {
                    finalQActions.forEach(action -> action.play(player));
                } catch (Exception exception) {
                    errors.add(new FriendlyError("Command", "Failed to execute action", exception.getMessage(), "").addStacktrace(exception.getStackTrace()));
                }
                if (!errors.isEmpty()) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Action konnte nicht ausgeführt werden:");
                    for (FriendlyError error : errors) {
                        MessageUtil.sendMessage(player, error.getMessage());
                    }
                }
            }
        };
        later.runTaskLater(QuestsXL.getInstance(), 20);
    }
}
