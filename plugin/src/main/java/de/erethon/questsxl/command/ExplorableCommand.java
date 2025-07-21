package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.Exploration;
import de.erethon.questsxl.livingworld.ExplorationSet;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.livingworld.explorables.PointOfInterest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ExplorableCommand extends ECommand {

    private final QuestsXL plugin = QuestsXL.getInstance();
    private final Exploration exploration = plugin.getExploration();

    public  ExplorableCommand() {
        setCommand("explorable");
        setAliases("e", "exp");
        setDescription("Manage explorable areas.");
        setPermission("questsxl.explorable");
        setUsage("/questsxl explorable <subcommand> [args]");
        setConsoleCommand(false);
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args[1].equalsIgnoreCase("set")) {
            if (args[2].equalsIgnoreCase("create")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set create <id>");
                    return;
                }
                if (exploration.getSet(args[3]) != null) {
                    MessageUtil.sendMessage(player, "<red>Explorable set with ID <gold>" + args[3] + "<red> already exists.");
                    return;
                }
                ExplorationSet set = new ExplorationSet(args[3]);
                exploration.addSet(set);
                MessageUtil.sendMessage(player, "<green>Created explorable set with ID <gold>" + set.id() + "<green>.");
                return;
            }
            if (args[2].equalsIgnoreCase("delete")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set delete <id>");
                    return;
                }
                ExplorationSet set = exploration.getSet(args[3]);
                if (set == null) {
                    MessageUtil.sendMessage(player, "<red>Explorable set with ID <gold>" + args[3] + "<red> does not exist.");
                    return;
                }
            }
            if (args[2].equalsIgnoreCase("add")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set add <id> <poi/chest/event/set>");
                    return;
                }
                ExplorationSet set = exploration.getSet(args[3]);
                if (set == null) {
                    MessageUtil.sendMessage(player, "<red>Explorable set with ID <gold>" + args[3] + "<red> does not exist.");
                    return;
                }
                if (args.length < 5) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set add <id> <poi/chest/event/set>");
                    return;
                }
                switch (args[4].toLowerCase()) {
                    case "poi" -> {
                        if (args.length < 6) {
                            MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set add <id> poi <name>");
                            return;
                        }
                        PointOfInterest poi = exploration.getPointOfInterest(args[5]);
                        if (poi == null) {
                            MessageUtil.sendMessage(player, "<red>POI with id <gold>" + args[5] + "<red> does not exist.");
                            return;
                        }
                        poi.setSet(set);
                        set.addExplorable(poi);
                        MessageUtil.sendMessage(player, "<green>Added POI <gold>" + poi.id() + "<green> to set <gold>" + set.id() + "<green>.");
                        return;
                    }
                    case "chest" -> {
                        // TODO: Loot chests
                    }
                    case "event" -> {
                        if (args.length < 6) {
                            MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set add <id> poi <name>");
                            return;
                        }
                        QEvent event = plugin.getEventManager().getByID(args[5]);
                        if (event == null) {
                            MessageUtil.sendMessage(player, "<red>Event with id <gold>" + args[5] + "<red> does not exist.");
                            return;
                        }
                        set.addExplorable(event);
                        MessageUtil.sendMessage(player, "<green>Added event <gold>" + event.id() + "<green> to set <gold>" + set.id() + "<green>.");
                        return;
                    }
                    default -> MessageUtil.sendMessage(player, "<red>Unknown type: " + args[4]);
                }
            }
            if (args[2].equalsIgnoreCase("parent")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set parent <id>");
                    return;
                }
                ExplorationSet set = exploration.getSet(args[3]);
                if (set == null) {
                    MessageUtil.sendMessage(player, "<red>Explorable set with ID <gold>" + args[3] + "<red> does not exist.");
                    return;
                }
                ExplorationSet other = exploration.getSet(args[4]);
                if (other == null) {
                    MessageUtil.sendMessage(player, "<red>Explorable set with ID <gold>" + args[4] + "<red> does not exist.");
                    return;
                }
                set.setParent(other);
                MessageUtil.sendMessage(player, "<green>Set parent of <gold>" + set.id() + "<green> to <gold>" + other.id() + "<green>.");
                return;
            }
            if (args[2].equalsIgnoreCase("list")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set list <id>");
                    return;
                }
                ExplorationSet set = exploration.getSet(args[3]);
                if (set == null) {
                    MessageUtil.sendMessage(player, "<red>Explorable set with ID <gold>" + args[3] + "<red> does not exist.");
                    return;
                }
                StringBuilder sb = new StringBuilder("<green>Explorables in set <gold>" + set.id() + "<green>: ");
                for (var explorable : set.getExplorables()) {
                    sb.append("<gold>").append(explorable.id()).append("<green>, ");
                }
                MessageUtil.sendMessage(player, sb.toString());
                return;
            }
            MessageUtil.sendMessage(player, "<red>Unknown subcommand for set: " + args[2] + " (available: create, delete, add, parent)");
        }
        if (args[1].equalsIgnoreCase("chest")) {
            Block block = player.getTargetBlockExact(5);
            if (block == null || block.getType() != Material.CHEST) {
                MessageUtil.sendMessage(player, "<red>You must be looking at a chest block to set it as a loot chest.");
                return;
            }
        }
        if (args[1].equalsIgnoreCase("poi")) {
            Location location = player.getLocation();
            if (args[2].equalsIgnoreCase("create")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable poi create <id>");
                    return;
                }
                String id = args[3];
                double radius = Double.parseDouble(args[4]);
                PointOfInterest poi = new PointOfInterest(id, location, radius);
                exploration.addPointOfInterest(poi);
                MessageUtil.sendMessage(player, "<green>Created POI <gold>" + id + "<green> at your location.");
            }
            if (args[2].equalsIgnoreCase("delete")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable poi delete <id>");
                    return;
                }
                String id = args[3];
                PointOfInterest poi = exploration.getPointOfInterest(id);
                if (poi == null) {
                    MessageUtil.sendMessage(player, "<red>POI with id <gold>" + id + "<red> does not exist.");
                    return;
                }
                exploration.removePointOfInterest(poi);
                MessageUtil.sendMessage(player, "<green>Deleted POI <gold>" + id + "<green>.");
                return;
            }
            if (args[2].equalsIgnoreCase("tp") || args[2].equalsIgnoreCase("teleport")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable poi teleport <id>");
                    return;
                }
                String id = args[3];
                PointOfInterest poi = exploration.getPointOfInterest(id);
                if (poi == null) {
                    MessageUtil.sendMessage(player, "<red>POI with id <gold>" + id + "<red> does not exist.");
                    return;
                }
                Location poiLocation = poi.location();
                if (poiLocation == null) {
                    MessageUtil.sendMessage(player, "<red>POI with id <gold>" + id + "<red> has no valid location.");
                    return;
                }
                if (poiLocation.getWorld() == null) {
                    MessageUtil.sendMessage(player, "<red>POI with id <gold>" + id + "<red> has no valid world.");
                    return;
                }
                player.teleportAsync(poiLocation);
                MessageUtil.sendMessage(player, "<green>Teleported to POI <gold>" + id + "<green>.");
            }
            MessageUtil.sendMessage(player, "<red>Unknown subcommand for poi: " + args[2] + " (available: create, delete,  teleport)");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completes = new java.util.ArrayList<>(List.of("set", "poi", "chest"));
            completes.removeIf(id -> !id.startsWith(args[1]));
            return completes;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
            List<String> completes = new java.util.ArrayList<>(List.of("create", "delete", "add", "parent", "list"));
            completes.removeIf(id -> !id.startsWith(args[2]));
            return completes;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("poi")) {
            List<String> completes = new java.util.ArrayList<>(List.of("create", "delete", "tp", "teleport"));
            completes.removeIf(id -> !id.startsWith(args[2]));
            return completes;
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("add")) {
            List<String> completes = new java.util.ArrayList<>(List.of("poi", "chest", "event"));
            completes.removeIf(id -> !id.startsWith(args[3]));
            return completes;
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("add") && args[3].equalsIgnoreCase("poi")) {
            return exploration.getPointOfInterestIDs().stream()
                    .filter(id -> id.startsWith(args[4]))
                    .toList();
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("parent")) {
            return exploration.getSetIDs().stream()
                    .filter(id -> id.startsWith(args[3]))
                    .toList();
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("list")) {
            return exploration.getSetIDs().stream()
                    .filter(id -> id.startsWith(args[3]))
                    .toList();
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("poi") && (args[2].equalsIgnoreCase("tp") || args[2].equalsIgnoreCase("teleport"))) {
            return exploration.getPointOfInterestIDs().stream()
                    .filter(id -> id.startsWith(args[3]))
                    .toList();
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("chest")) {
            List<String> completes = new java.util.ArrayList<>(List.of("create", "delete"));
            completes.removeIf(id -> !id.startsWith(args[3]));
            return completes;
        }
        return super.onTabComplete(sender, args);
    }
}
