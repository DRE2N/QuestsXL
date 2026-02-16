package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.region.QRegion;
import de.erethon.questsxl.region.QRegionManager;
import de.erethon.questsxl.region.RegionFlag;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RegionCommand extends ECommand {

    QuestsXL plugin = QuestsXL.get();
    QRegionManager manager = plugin.getRegionManager();

    public RegionCommand() {
        setCommand("region");
        setAliases("rg");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.admin.region");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        Location location = player.getLocation();
        if (args.length < 2) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Unbekannter Befehl. Probiere z.B. /q rg i");
            return;
        }

        if (args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("i")) {
            QRegion region = manager.getByLocation(location);
            if (region == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Keine Region gefunden.");
                return;
            }
            MessageUtil.sendMessage(player, "&7Region: &6" + region.getId());
            MessageUtil.sendMessage(player, region.getNiceLocation());
            String commaSeparatedPublic = region.getPublicFlags().stream()
                    .map(RegionFlag::name)
                    .collect(Collectors.joining("&8, "));
            MessageUtil.sendMessage(player, "&7Public-Flags: &6" + commaSeparatedPublic);
            String commaSeparatedQuest = region.getQuestFlags().stream()
                    .map(RegionFlag::name)
                    .collect(Collectors.joining("&8, "));
            MessageUtil.sendMessage(player, "&7Quest-Flags: &6" + commaSeparatedQuest);
            MessageUtil.sendMessage(player, "&7Instanced: &6" + (region.isInstanced() ? "Yes (proximity: " + region.getProximityDistance() + " chunks)" : "No"));
            if (region.getLinkedQuest() == null) {
                MessageUtil.sendMessage(player, "&7Quest: &6Keine");
                return;
            }
            MessageUtil.sendMessage(player, "&7Quest: &6" + region.getLinkedQuest().getName());
            return;
        }

        if (args[1].equalsIgnoreCase("pos1") || args[1].equalsIgnoreCase("p1")) {
            Location blockLoc = location.getBlock().getLocation();
            manager.setPos1(player, blockLoc);
            MessageUtil.sendMessage(player, "<green>Position 1 wurde auf <yellow>" +
                    blockLoc.getBlockX() + ", " + blockLoc.getBlockY() + ", " + blockLoc.getBlockZ() + " <green>gesetzt.");

            Location pos2 = manager.getPos2(player);
            if (pos2 != null) {
                showSelectionInfo(player, blockLoc, pos2);
            } else {
                MessageUtil.sendMessage(player, "<gray>Setze nun Position 2 mit /q rg pos2");
            }
            return;
        }

        if (args[1].equalsIgnoreCase("pos2") || args[1].equalsIgnoreCase("p2")) {
            Location blockLoc = location.getBlock().getLocation();
            manager.setPos2(player, blockLoc);
            MessageUtil.sendMessage(player, "<green>Position 2 wurde auf <yellow>" +
                    blockLoc.getBlockX() + ", " + blockLoc.getBlockY() + ", " + blockLoc.getBlockZ() + " <green>gesetzt.");

            Location pos1 = manager.getPos1(player);
            if (pos1 != null) {
                showSelectionInfo(player, pos1, blockLoc);
            } else {
                MessageUtil.sendMessage(player, "<gray>Setze nun Position 1 mit /q rg pos1");
            }
            return;
        }

        if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("c")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe eine ID an");
                return;
            }

            // Check if region already exists
            if (manager.getByID(args[2]) != null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Region existiert bereits.");
                return;
            }

            // Check if player has both positions set
            Location pos1 = manager.getPos1(player);
            Location pos2 = manager.getPos2(player);

            if (pos1 == null || pos2 == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Du musst zuerst beide Positionen setzen!");
                MessageUtil.sendMessage(player, "<gray>Verwende /q rg pos1 und /q rg pos2");
                return;
            }

            // Check if positions are in the same world
            if (!pos1.getWorld().equals(pos2.getWorld())) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Beide Positionen müssen in der gleichen Welt sein!");
                return;
            }

            // Create the region with the positions
            QRegion region = new QRegion(args[2]);
            region.setPos1(pos1);
            region.setPos2(pos2);
            manager.getRegions().add(region);
            manager.save();

            // Clear the player's selection
            manager.clearSelection(player);

            MessageUtil.sendMessage(player, "<green>Region <yellow>" + args[2] + " <green>erfolgreich erstellt.");
            showSelectionInfo(player, pos1, pos2);
            return;
        }

        if (args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase("ls")) {
            if (manager.getRegions().isEmpty()) {
                MessageUtil.sendMessage(player, "<gray>Keine Regionen gefunden.");
                return;
            }
            MessageUtil.sendMessage(player, "<gray>Regionen:");
            for (QRegion region : manager.getRegions()) {
                MessageUtil.sendMessage(player, "&8- &6" + region.getId() + "&8: " + region.getNiceLocation());
            }
            return;
        }

        if (args[1].equalsIgnoreCase("instanced")) {
            if (args.length < 4) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Verwendung: /q rg instanced <region-id> <true|false>");
                return;
            }
            QRegion region = manager.getByID(args[2]);
            if (region == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Region existiert nicht.");
                return;
            }
            boolean instanced = Boolean.parseBoolean(args[3]);
            region.setInstanced(instanced);

            if (instanced) {
                // Create instance template for the region
                if (manager.getInstanceService() != null) {
                    manager.getInstanceService().createTemplateForRegion(region);
                    MessageUtil.sendMessage(player, "<green>Region " + region.getId() + " wurde auf 'instanced' gesetzt.");
                    MessageUtil.sendMessage(player, "<gray>Template-ID: " + region.getInstanceTemplateId());
                    MessageUtil.sendMessage(player, "<gray>Proximity-Distanz: " + region.getProximityDistance() + " chunks");
                } else {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Instance-Service ist nicht verfügbar.");
                }
            } else {
                MessageUtil.sendMessage(player, "<green>Region " + region.getId() + " ist nicht mehr 'instanced'.");
            }
            manager.save();
            return;
        }

        if (args[1].equalsIgnoreCase("proximity")) {
            if (args.length < 4) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Verwendung: /q rg proximity <region-id> <chunks>");
                return;
            }
            QRegion region = manager.getByID(args[2]);
            if (region == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Region existiert nicht.");
                return;
            }
            try {
                int distance = Integer.parseInt(args[3]);
                if (distance < 0) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Distanz muss positiv sein.");
                    return;
                }
                region.setProximityDistance(distance);
                MessageUtil.sendMessage(player, "<green>Proximity-Distanz für Region " + region.getId() + " wurde auf " + distance + " chunks gesetzt.");
                manager.save();
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Ungültige Zahl: " + args[3]);
            }
            return;
        }

        MessageUtil.sendMessage(player, QuestsXL.ERROR + "Unbekannter Befehl. Probiere z.B. /q rg i");
    }

    private void showSelectionInfo(Player player, Location pos1, Location pos2) {
        int dx = Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1;
        int dy = Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1;
        int dz = Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1;
        int volume = dx * dy * dz;

        MessageUtil.sendMessage(player, "<gray>Selektion: <yellow>" + dx + " x " + dy + " x " + dz +
                " <gray>(<yellow>" + volume + " <gray>Blöcke)");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> actions = new ArrayList<>(Arrays.asList(
                    "info", "i",
                    "create", "c",
                    "pos1", "p1",
                    "pos2", "p2",
                    "list", "ls",
                    "instanced",
                    "proximity"
            ));
            actions.removeIf(a -> !a.toLowerCase().startsWith(args[1].toLowerCase()));
            return actions;
        }

        if (args.length == 3) {
            String action = args[1].toLowerCase();

            if (action.equals("instanced") || action.equals("proximity")) {
                // Suggest existing region IDs
                List<String> regionIds = new ArrayList<>();
                for (QRegion region : manager.getRegions()) {
                    regionIds.add(region.getId());
                }
                regionIds.removeIf(id -> !id.toLowerCase().startsWith(args[2].toLowerCase()));
                return regionIds;
            }

            if (action.equals("create") || action.equals("c")) {
                return List.of("<region-id>");
            }
        }

        if (args.length == 4) {
            String action = args[1].toLowerCase();

            if (action.equals("instanced")) {
                List<String> options = new ArrayList<>(Arrays.asList("true", "false"));
                options.removeIf(o -> !o.startsWith(args[3].toLowerCase()));
                return options;
            }

            if (action.equals("proximity")) {
                return List.of("<chunks>", "0", "1", "2", "3");
            }
        }

        return List.of();
    }
}