package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.TalkAction;
import de.erethon.questsxl.gui.QuestBook;
import de.erethon.questsxl.instancing.InstancedBlockCollection;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TestCommand extends ECommand {
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
            QPlayer qPlayer = QuestsXL.get().getDatabaseManager().getCurrentPlayer(player);
            qPlayer.setInConversation(true);
            player.sendMessage("Hello");
            player.sendMessage("Hello1");
            player.sendMessage("Hello2");
            player.sendMessage("Hello3");
            player.sendMessage("Hello4");
            player.sendMessage("Hello5");
            player.sendMessage("Hello6");
            qPlayer.setInConversation(false);

            BukkitRunnable later = new BukkitRunnable() {
                @Override
                public void run() {
                    qPlayer.sendMessagesInQueue(false);
                }
            };
            later.runTaskLater(QuestsXL.get(), 20);
            return;
        }
        if (args[1].equals("conv")) {
            new TalkAction().play(QuestsXL.get().getDatabaseManager().getCurrentPlayer(player));
            return;
        }
        MessageUtil.sendMessage(player, "Invalid test command.");
    }
}
