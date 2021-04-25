package de.erethon.questsxl.commands;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import de.erethon.questsxl.action.QCutscene;
import de.erethon.questsxl.gui.QuestBook;
import de.erethon.questsxl.instancing.InstancedBlockCollection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TestCommand extends DRECommand {

    List<Location> locationList = new ArrayList<>();
    InstancedBlockCollection collection = new InstancedBlockCollection();

    public TestCommand() {
        setCommand("test");
        setAliases("t");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.test");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args[1].equals("play") || args[1].equals("p")) {
            QCutscene scene = new QCutscene();
            scene.setLocs(locationList);
            scene.play(player);
            return;
        }
        if (args[1].equals("a")) {
            locationList.add(player.getLocation());
            MessageUtil.sendMessage(player, "Added");
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
        MessageUtil.sendMessage(player, "Invalid test command.");
    }
}
