package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.EventState;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EventCommand extends ECommand {

    QuestsXL plugin = QuestsXL.get();

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
        if (args.length > 2 && (args[2].equalsIgnoreCase("teleport") || args[2].equalsIgnoreCase("tp"))) {
            Player player = (Player) commandSender;
            player.teleport(event.getLocation());
            MessageUtil.sendMessage(commandSender, "&7Du wurdest zum Event &a" + event.getName() + " &7teleportiert.");
            return;
        }
        if (args.length > 2 && args[2].equalsIgnoreCase("info")) {
            try {
                MessageUtil.sendMessage(commandSender, "&8&m               &r &a" + event.getName() + " &8&m               &r");
                MessageUtil.sendMessage(commandSender, "&7Location&8: &a" + event.getLocation().getX() + " &8/&a " + event.getLocation().getY() + " &8/&a " + event.getLocation().getZ());
                MessageUtil.sendMessage(commandSender, "&7Range&8: &a" + event.getRange());
                MessageUtil.sendMessage(commandSender, "&7State&8: &a" + event.getState());
                if (event.getCurrentStage() == null) {
                    MessageUtil.sendMessage(commandSender, "&7Stage&8: &aNone");
                } else {
                    MessageUtil.sendMessage(commandSender, "&7Stage&8: &a" + event.getCurrentStage().getId());
                }
                Component players = getPlayerHoverParticipation(event);
                MessageUtil.sendMessage(commandSender, players);
                if (event.getTopPlayer() == null) {
                    MessageUtil.sendMessage(commandSender, "&7Top Player&8: &aNone");
                } else {
                    MessageUtil.sendMessage(commandSender, "&7Top Player&8: &a" + event.getTopPlayer().getName());
                }
            }
            catch (Exception e) {
                MessageUtil.sendMessage(commandSender, QuestsXL.ERROR + "Ein Fehler ist aufgetreten: " + e.getMessage());
            }
            return;
        }
        MessageUtil.sendMessage(commandSender, QuestsXL.ERROR + "Bitte gebe eine gültige Aktion an: active, complete, disable, inactive, info");
    }

    private static @NotNull Component getPlayerHoverParticipation(QEvent event) {
        Component players = Component.text("Players in Range", NamedTextColor.GRAY).append(Component.text(":", NamedTextColor.DARK_GRAY));
        Component playerHover = Component.empty();
        for (QPlayer player : event.getPlayersInRange()) {
            int participation =  event.getEventParticipation(player);
            playerHover = playerHover.append(Component.text(player.getName(), NamedTextColor.GREEN))
                    .append(Component.text(": ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(participation, NamedTextColor.GREEN)));
        }
        players = players.append(Component.text(" " + event.getPlayersInRange().size(), NamedTextColor.GREEN).hoverEvent(playerHover));
        return players;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> ids = plugin.getEventManager().getEventIDs();
            ids.removeIf(id -> !id.startsWith(args[1]));
            return ids;
        }
        if (args.length == 3) {
            List<String> completes = new java.util.ArrayList<>(List.of("active", "complete", "disable", "inactive", "info", "teleport"));
            completes.removeIf(id -> !id.startsWith(args[2]));
            return completes;
        }
        return super.onTabComplete(sender, args);
    }
}
