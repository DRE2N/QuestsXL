package de.erethon.questsxl.commands;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QTalkAction;
import de.erethon.questsxl.gui.QuestBook;
import de.erethon.questsxl.instancing.InstancedBlockCollection;
import de.erethon.questsxl.players.QPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TestCommand extends DRECommand {
    InstancedBlockCollection collection = new InstancedBlockCollection();

    public TestCommand() {
        setCommand("test");
        setAliases("t");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.admin.test");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args.length < 2) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Unbekannter Befehl. Probiere z.B. /q rg i");
            return;
        }
        if (args[1].equals("pos1")) {
            collection.setPos1(player.getLocation());
            MessageUtil.sendMessage(player, "Pos1 set");
            return;
        }
        if (args[1].equals("pos2")) {
            collection.setPos2(player.getLocation());
            MessageUtil.sendMessage(player, "Pos2 set");
            return;
        }
        if (args[1].equals("saveHidden")) {
            collection.saveHidden();
            MessageUtil.sendMessage(player, "Saved hidden state");
            return;
        }
        if (args[1].equals("saveShown")) {
            collection.saveShown();
            MessageUtil.sendMessage(player, "Saved shown state");
            return;
        }
        if (args[1].equals("show")) {
            collection.show(player);
            MessageUtil.sendMessage(player, "Shown");
            return;
        }
        if (args[1].equals("hide")) {
            collection.hide(player);
            MessageUtil.sendMessage(player, "Hidden");
            return;
        }
        if (args[1].equals("book") || args[1].equals("b")) {
            QuestBook.write(player);
            return;
        }
        if (args[1].equals("text")) {
            QPlayer qPlayer = QuestsXL.getInstance().getPlayerCache().get(player);
            qPlayer.setInConversation(true);
            player.sendMessage("Hello");
            player.sendMessage("Hello1");
            player.sendMessage("Hello2");
            player.sendMessage("Hello3");
            qPlayer.sendConversationMsg("<gray>[1/3]<green> Test test test");
            qPlayer.sendConversationMsg("<gray>[2/3]<green> Test test ");
            qPlayer.sendConversationMsg("<gray>[3/3]<green> Test ");
            player.sendMessage("Hello4");
            player.sendMessage("Hello5");
            player.sendMessage("Hello6");
            qPlayer.setInConversation(false);

            BukkitRunnable later = new BukkitRunnable() {
                @Override
                public void run() {
                    qPlayer.sendMessagesInQueue();
                }
            };
            later.runTaskLater(QuestsXL.getInstance(), 20);
            return;
        }
        if (args[1].equals("conv")) {
            new QTalkAction().play(player);
            return;
        }
        MessageUtil.sendMessage(player, "Invalid test command.");
    }
}
