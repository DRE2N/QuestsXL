package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.DummyAction;
import de.erethon.questsxl.animation.AnimationManager;
import de.erethon.questsxl.animation.QCutscene;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CutsceneCommand extends ECommand {


        QuestsXL plugin = QuestsXL.get();
        AnimationManager manager = plugin.getAnimationManager();
        QCutscene scene = null;
        List<Location> locationList = new ArrayList<>();
        List<String> messages = new ArrayList<>();


    public CutsceneCommand() {
            setCommand("cutscene");
            setAliases("c");
            setMinArgs(-1);
            setMaxArgs(-1);
            setPlayerCommand(true);
            setHelp("Help.");
            setPermission("qxl.admin.cutscene");
        }

        @Override
        public void onExecute(String[] args, CommandSender commandSender) {
            Player player = (Player) commandSender;
            if (args.length < 2) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe einen Befehl an.");
                return;
            }
            if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("c") ) {
                if (args.length < 3) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe einen Namen an.");
                    return;
                }
                if (manager.getCutscene(args[2]) != null) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Cutscene existiert bereits.");
                    return;
                }
                if (scene != null) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Es wird bereits eine Cutscene erstellt - wahrscheinlich von einem anderen Teamler, blame them ;)");
                    return;
                }
                scene = new QCutscene(args[2]);
                MessageUtil.sendMessage(player, "&7Cutscene &a" + args[2] + " &7erstellt.");
                return;
            }
            if (args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase("ls") ) {
                QCutscene cutscene;
                if (args.length > 2) {
                    cutscene = manager.getCutscene(args[2]);
                } else {
                    cutscene = scene;
                }
                if (cutscene == null) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Cutscene nicht gefunden.");
                    return;
                }
                MessageUtil.sendMessage(player, "&7Locations:");
                int num = 0;
                for (Location location : locationList) {
                    int x = location.getBlockX();
                    int y = location.getBlockY();
                    int z = location.getBlockZ();
                    MessageUtil.sendMessage(player, "<click:run_command:/tp " + x + " " + y + " " + z + ">&8" + num + "&7: &6" + z + "&8/&6" + y + "&8/&6" + z + "</click>");
                    num++;
                }
                MessageUtil.sendMessage(player, "&8&oKlicke eine Location an, um dich zu teleportieren.");
                return;
            }
            if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("a") ) {
                if (scene == null) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte erstelle erst eine Cutscene. - /q cutscene create <Name>");
                    return;
                }
                locationList.add(player.getLocation());
                MessageUtil.sendMessage(player, "&7Location hinzugefügt");
                return;
            }
            if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("r") ) {
                if (scene == null) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte erstelle erst eine Cutscene. - /q cutscene create <Name>");
                    return;
                }
                if (args.length < 3) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe eine ID an.");
                    return;
                }
                int id = Integer.parseInt(args[2]);
                locationList.remove(Integer.parseInt(args[2]));
                MessageUtil.sendMessage(player, "&7Location gelöscht.");
                return;
            }
            if (args[1].equalsIgnoreCase("play") || args[1].equalsIgnoreCase("p") ) {
                if (args.length < 3) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe eine ID an.");
                    return;
                }
                QCutscene cutscene = manager.getCutscene(args[2]);
                if (cutscene == null) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Cutscene existiert nicht.");
                    return;
                }
                cutscene.play(player, new DummyAction());
                MessageUtil.sendMessage(player, "&7Spiele Cutscene &a" + args[2] + " &7ab...");
                return;
            }
            if (args[1].equalsIgnoreCase("save") || args[1].equalsIgnoreCase("s") ) {
                if (scene == null) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte erstelle erst eine Cutscene. - /q cutscene create <Name>");
                    return;
                }
                scene.setLocs(locationList);
                manager.getCutscenes().add(scene);
                manager.save();
                MessageUtil.sendMessage(player, "&7Cutscene &a" + scene.getId() + " &7mit &a " + scene.getLocations().size() + " &7Punkten gespeichert.");
                locationList.clear();
                scene = null;
                return;
            }
            if (args[1].equalsIgnoreCase("listScenes")) {
                MessageUtil.sendMessage(player, "&7Cutscenes: ");
                for (QCutscene cutscene : manager.getCutscenes()) {
                    Location location = cutscene.getLocations().get(0);
                    int x = location.getBlockX();
                    int y = location.getBlockY();
                    int z = location.getBlockZ();
                    MessageUtil.sendMessage(player, "<click:run_command:/tp " + x + " " + y + " " + z + ">&8" + cutscene.getId() + "&7: &6" + z + "&8/&6" + y + "&8/&6" + z + "</click>");
                }
                MessageUtil.sendMessage(player, "&8&oKlicke eine Cutscene an, um dich zum Start zu teleportieren.");
            }
        }
}
