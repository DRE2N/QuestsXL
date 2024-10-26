package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.dialogue.ActiveDialogue;
import de.erethon.questsxl.dialogue.QDialogue;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fyreum
 */
public class DialogueCommand extends ECommand {

    QuestsXL plugin = QuestsXL.getInstance();

    final List<String> subs = List.of("start", "next");

    public DialogueCommand() {
        setCommand("dialogue");
        setAliases("d");
        setMinArgs(1);
        setMaxArgs(2);
        setPlayerCommand(true);
        setUsage("/q dialogue [start|next] ([dialogue])");
        setDescription("Befehl zum Starten und skippen von Dialogen");
        setDefaultHelp();
        setPermission("qxl.user.dialogue");
    }

    @Override
    public void onExecute(String[] args, CommandSender sender) {
        Player player = (Player) sender;
        if (args[1].equalsIgnoreCase("start")) {
            if (args.length != 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe einen Dialog an.");
                return;
            }
            QDialogue dialogue = plugin.getDialogueManager().get(args[2]);
            if (dialogue == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Der angegebene Dialog konnte nicht gefunden werden.");
                return;
            }
            dialogue.start(plugin.getPlayerCache().getByPlayer(player));
        } else if (args[1].equalsIgnoreCase("next")) {
            QPlayer qPlayer = plugin.getPlayerCache().getByPlayer(player);
            ActiveDialogue activeDialogue = qPlayer.getActiveDialogue();
            if (activeDialogue == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Du hast derzeit keinen aktiven Dialog.");
                return;
            }
            activeDialogue.continueDialogue();
        } else {
            MessageUtil.sendMessage(player, getHelp());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completes = new ArrayList<>(subs.size());
            for (String sub : subs) {
                if (sub.startsWith(args[1].toLowerCase())) {
                    completes.add(sub);
                }
            }
            return completes;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("start")) {
            List<String> completes = new ArrayList<>();
            for (QDialogue dialogue : plugin.getDialogueManager()) {
                if (dialogue.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completes.add(dialogue.getName());
                }
            }
            return completes;
        }
        return null;
    }
}
