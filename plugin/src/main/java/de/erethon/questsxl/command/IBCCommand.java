package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.instancing.BlockCollectionManager;
import de.erethon.questsxl.instancing.InstancedBlockCollection;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IBCCommand extends ECommand {

    QuestsXL plugin = QuestsXL.get();
    BlockCollectionManager manager = plugin.getBlockCollectionManager();
    InstancedBlockCollection ibc = null;

    public IBCCommand() {
        setCommand("ibc");
        setAliases("block");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.admin.ibc");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args.length < 2) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe einen Befehl an.");
            return;
        }
        if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("c")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe einen Namen an.");
                return;
            }
            if (manager.getByID(args[2]) != null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese IBC existiert bereits.");
                return;
            }
            if (ibc != null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Es wird bereits eine IBC erstellt - wahrscheinlich von einem anderen Teamler, blame them ;)");
                return;
            }
            ibc = new InstancedBlockCollection(args[2]);
            MessageUtil.sendMessage(player, "&7IBC &a" + args[2] + " &7erstellt.");
            return;
        }
        if (args[1].equalsIgnoreCase("pos1") || args[1].equalsIgnoreCase("p1")) {
            if (ibc == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte erstelle erst eine IBC - /q ibc create <Name>");
                return;
            }
            ibc.setPos1(player.getLocation());
            MessageUtil.sendMessage(player, "&7Pos1 für &a" + ibc.getId() + " &7gesetzt.");
            return;
        }
        if (args[1].equalsIgnoreCase("pos2") || args[1].equalsIgnoreCase("p2")) {
            if (ibc == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte erstelle erst eine IBC - /q ibc create <Name>");
                return;
            }
            ibc.setPos2(player.getLocation());
            MessageUtil.sendMessage(player, "&7Pos2 für &a" + ibc.getId() + " &7gesetzt.");
            return;
        }
        if (args[1].equalsIgnoreCase("saveShown") || args[1].equalsIgnoreCase("sS")) {
            if (ibc == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte erstelle erst eine IBC - /q ibc create <Name>");
                return;
            }
            if (ibc.getPos1() == null || ibc.getPos2() == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte setze erst die Ecken - /q ibc pos1/pos2");
                return;
            }
            ibc.saveShown();
            MessageUtil.sendMessage(player, "&7Sichtbar-Zustand für &a" + ibc.getId() + " &7gespeichert.");
            return;
        }
        if (args[1].equalsIgnoreCase("saveHidden") || args[1].equalsIgnoreCase("sH")) {
            if (ibc == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte erstelle erst eine IBC - /q ibc create <Name>");
                return;
            }
            if (ibc.getPos1() == null || ibc.getPos2() == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte setze erst die Ecken - /q ibc pos1/pos2");
                return;
            }
            ibc.saveHidden();
            MessageUtil.sendMessage(player, "&7Versteckt-Zustand für &a" + ibc.getId() + " &7gespeichert.");
            return;
        }
        if (args[1].equalsIgnoreCase("save")) {
            manager.getCollections().add(ibc);
            manager.save();
            MessageUtil.sendMessage(player, "&7IBC &a" + ibc.getId() + " &7gespeichert.");
            ibc = null;
            return;
        }
        if (args[1].equalsIgnoreCase("show") || args[1].equalsIgnoreCase("s")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe einen Namen an.");
                return;
            }
            manager.getByID(args[2]).show(player);
            MessageUtil.sendMessage(player, "&7IBC &a" + args[2] + " &7angezeigt.");
            return;
        }
        if (args[1].equalsIgnoreCase("hide") || args[1].equalsIgnoreCase("h")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe einen Namen an.");
                return;
            }
            manager.getByID(args[2]).hide(player);
            MessageUtil.sendMessage(player, "&7IBC &a" + args[2] + " &7versteckt.");
            return;
        }
        if (args[1].equalsIgnoreCase("reset") || args[1].equalsIgnoreCase("r")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe einen Namen an.");
                return;
            }
            manager.getByID(args[2]).reset(player);
            MessageUtil.sendMessage(player, "&7IBC &a" + args[2] + " &7zu Welt-Status resettet.");
            return;
        }
        MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe einen gültigen Befehl an.");
    }
}
