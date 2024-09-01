package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.EventState;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class EventCommand extends ECommand {

    QuestsXL plugin = QuestsXL.getInstance();

    public EventCommand() {
        setCommand("event");
        setAliases("e");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.admin.event");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        if (args.length < 2) {
            MessageUtil.sendMessage(commandSender, QuestsXL.ERROR + "Bitte gebe eine Event-ID an.");
            return;
        }
        QEvent event = plugin.getEventManager().getByID(args[1]);
        if (event == null) {
            MessageUtil.sendMessage(commandSender, QuestsXL.ERROR + "Dieses Event existiert nicht.");
            return;
        }
        if (args.length > 2 && args[2].equalsIgnoreCase("active")) {
            event.startFromAction(true);
            MessageUtil.sendMessage(commandSender, "&7Das Event &a" + event.getName() + " &7ist nun aktiv");
            return;
        }
        if (args.length > 2 && args[2].equalsIgnoreCase("complete")) {
            event.setState(EventState.COMPLETED);
            event.update();
            MessageUtil.sendMessage(commandSender, "&7Das Event &a" + event.getName() + " &7wurde abgeschlossen.");
            return;
        }
        if (args.length > 2 && args[2].equalsIgnoreCase("disable")) {
            event.setState(EventState.DISABLED);
            event.update();
            MessageUtil.sendMessage(commandSender, "&7Das Event &a" + event.getName() + " &7wurde deaktiviert.");
            return;
        }
        if (args.length > 2 && args[2].equalsIgnoreCase("inactive")) {
            event.setState(EventState.NOT_STARTED);
            event.update();
            MessageUtil.sendMessage(commandSender, "&7Das Event &a" + event.getName() + " &7ist nun inaktiv.");
            return;
        }
        if (args.length > 2 && args[2].equalsIgnoreCase("info")) {
            MessageUtil.sendMessage(commandSender, "&8&m               &r &a" + event.getName() + " &8&m               &r");
            MessageUtil.sendMessage(commandSender, "&7Location&8: &a" + event.getLocation());
            MessageUtil.sendMessage(commandSender, "&7Range&8: &a" + event.getRange());
            MessageUtil.sendMessage(commandSender, "&7State&8: &a" + event.getState());
            MessageUtil.sendMessage(commandSender, "&7Stage&8: &a" + event.getCurrentStage().getId());
            MessageUtil.sendMessage(commandSender, "&7Players&8: &a" + event.getPlayersInRange().size());
            MessageUtil.sendMessage(commandSender, "&7Top Player&8: &a" + event.getTopPlayer());
            return;
        }
    }
}
