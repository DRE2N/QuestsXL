package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.livingworld.Explorable;
import de.erethon.questsxl.livingworld.Exploration;
import de.erethon.questsxl.livingworld.ExplorationManager;
import de.erethon.questsxl.livingworld.ExplorationSet;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.livingworld.explorables.LootChest;
import de.erethon.questsxl.livingworld.explorables.PointOfInterest;
import de.erethon.questsxl.respawn.RespawnPoint;
import de.erethon.questsxl.respawn.RespawnPointUnlockMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ExplorableCommand extends ECommand {

    private final QuestsXL plugin = QuestsXL.get();
    private final Exploration exploration = plugin.getExploration();

    public  ExplorableCommand() {
        setCommand("explorable");
        setAliases("e", "exp");
        setDescription("Manage explorable areas.");
        setPermission("questsxl.explorable");
        setUsage("/questsxl explorable <subcommand> [args]");
        setMinArgs(0);
        setMaxArgs(Integer.MAX_VALUE);
        setConsoleCommand(false);
        setPlayerCommand(true);
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;

        ExplorationManager explorationManager = plugin.getExplorationManager();
        if (explorationManager == null) {
            MessageUtil.sendMessage(player, "<red>Exploration system is not yet initialized. Please try again in a moment.");
            return;
        }

        if (args.length <= 1) {
            MessageUtil.sendMessage(player, "<gold>Usage: /qxl explorable <subcommand>");
            MessageUtil.sendMessage(player, "<gray>Available subcommands:");
            MessageUtil.sendMessage(player, "<gray> - <gold>set <create|delete|add|parent|list|setname|setdesc> <id> [args]<gray> - Manage exploration sets");
            MessageUtil.sendMessage(player, "<gray> - <gold>poi <create|delete|tp|setname|setdesc> <id> <radius> <set_id> [args]<gray> - Manage points of interest");
            MessageUtil.sendMessage(player, "<gray> - <gold>respawn <create|delete|tp|list|setname|setdesc> <id> [args]<gray> - Manage respawn points");
            MessageUtil.sendMessage(player, "<gray> - <gold>chest <create|delete|setname|setdesc> <id> <set_id><gray> - Create loot chest (look at chest block)");
            return;
        }

        if (args[1].equalsIgnoreCase("set")) {
            if (args.length <= 2) {
                MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set <create|delete|add|parent|list|setname|setdesc> <id> [args]");
                return;
            }
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
                explorationManager.addExplorationSet(set);
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
                explorationManager.removeExplorationSet(set);
                MessageUtil.sendMessage(player, "<green>Deleted explorable set with ID <gold>" + args[3] + "<green>.");
                return;
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
                    case "respawn" -> {
                        if (args.length < 6) {
                            MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set add <id> respawn <respawn_point_id>");
                            return;
                        }
                        Explorable explorable = exploration.getStandaloneExplorable(args[5]);
                        if (!(explorable instanceof RespawnPoint respawnPoint)) {
                            MessageUtil.sendMessage(player, "<red>Respawn point with id <gold>" + args[5] + "<red> does not exist or is not explorable.");
                            return;
                        }
                        if (respawnPoint == null) {
                            MessageUtil.sendMessage(player, "<red>Explorable respawn point with id <gold>" + args[5] + "<red> does not exist.");
                            return;
                        }
                        set.addExplorable(respawnPoint);
                        respawnPoint.setSet(set);
                        MessageUtil.sendMessage(player, "<green>Added respawn point <gold>" + respawnPoint.id() + "<green> to set <gold>" + set.id() + "<green>.");
                        return;
                    }
                    default -> MessageUtil.sendMessage(player, "<red>Unknown type: " + args[4]);
                }
            }
            if (args[2].equalsIgnoreCase("parent")) {
                if (args.length < 5) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set parent <id> <parent_id>");
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
            if (args[2].equalsIgnoreCase("setname")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set setname <id> <name>");
                    return;
                }
                ExplorationSet set = exploration.getSet(args[3]);
                if (set == null) {
                    MessageUtil.sendMessage(player, "<red>Explorable set with ID <gold>" + args[3] + "<red> does not exist.");
                    return;
                }

                // Collect remaining arguments to allow spaces in the name
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 4; i < args.length; i++) {
                    if (i > 4) nameBuilder.append(" ");
                    nameBuilder.append(args[i]);
                }
                String nameString = nameBuilder.toString();

                set.setDisplayName(QTranslatable.fromString(nameString));
                MessageUtil.sendMessage(player, "<green>Set display name of set <gold>" + set.id() + "<green> to: <gold>" + nameString);
                return;
            }
            if (args[2].equalsIgnoreCase("setdesc")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable set setdesc <id> <description>");
                    return;
                }
                ExplorationSet set = exploration.getSet(args[3]);
                if (set == null) {
                    MessageUtil.sendMessage(player, "<red>Explorable set with ID <gold>" + args[3] + "<red> does not exist.");
                    return;
                }

                // Collect remaining arguments to allow spaces in the description
                StringBuilder descriptionBuilder = new StringBuilder();
                for (int i = 4; i < args.length; i++) {
                    if (i > 4) descriptionBuilder.append(" ");
                    descriptionBuilder.append(args[i]);
                }
                String descriptionString = descriptionBuilder.toString();

                set.setDescription(QTranslatable.fromString(descriptionString));
                MessageUtil.sendMessage(player, "<green>Set description of set <gold>" + set.id() + "<green> to: <gold>" + descriptionString);
                return;
            }
            MessageUtil.sendMessage(player, "<red>Unknown subcommand for set: " + args[2] + " (available: create, delete, add, parent, setname, setdesc)");
        }
        if (args[1].equalsIgnoreCase("chest")) {
            if (args.length <= 2) {
                MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable chest <create|delete> <id> <set_id>");
                return;
            }

            Block block = player.getTargetBlockExact(5);
            if (block == null || (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST)) {
                MessageUtil.sendMessage(player, "<red>You must be looking at a chest block to set it as a loot chest.");
                return;
            }

            if (args[2].equalsIgnoreCase("create")) {
                if (args.length < 5) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable chest create <id> <set_id>");
                    return;
                }

                String chestId = args[3];
                String setId = args[4];

                ExplorationSet set = exploration.getSet(setId);
                if (set == null) {
                    MessageUtil.sendMessage(player, "<red>Exploration set with ID <gold>" + setId + "<red> does not exist.");
                    return;
                }

                if (plugin.getLootChestManager().getLootChest(chestId) != null) {
                    MessageUtil.sendMessage(player, "<red>Loot chest with ID <gold>" + chestId + "<red> already exists.");
                    return;
                }

                LootChest lootChest = plugin.getLootChestManager().createLootChest(chestId, block, set);
                if (lootChest != null) {
                    set.addExplorable(lootChest);
                    exploration.addLootChest(lootChest);
                    explorationManager.save();

                    MessageUtil.sendMessage(player, "<green>Created loot chest <gold>" + chestId + "<green> with <gold>" + lootChest.getLootItems().size() + "<green> items in set <gold>" + setId + "<green>.");
                } else {
                    MessageUtil.sendMessage(player, "<red>Failed to create loot chest. Make sure you're looking at a chest block.");
                }
                return;
            }

            if (args[2].equalsIgnoreCase("delete")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable chest delete <id>");
                    return;
                }

                String chestId = args[3];
                LootChest lootChest = plugin.getLootChestManager().getLootChest(chestId);

                if (lootChest == null) {
                    MessageUtil.sendMessage(player, "<red>Loot chest with ID <gold>" + chestId + "<red> does not exist.");
                    return;
                }

                ExplorationSet set = exploration.getSetContaining(lootChest);
                if (set != null) {
                    set.removeExplorable(lootChest);
                }

                String currentChestId = plugin.getLootChestManager().getChestId(block);
                if (chestId.equals(currentChestId)) {
                    Chest chestState = (Chest) block.getState();
                    chestState.getPersistentDataContainer().remove(plugin.getLootChestManager().getLootChestKey());
                    chestState.update();
                }

                plugin.getLootChestManager().reloadLootChests();
                explorationManager.save();

                MessageUtil.sendMessage(player, "<green>Deleted loot chest <gold>" + chestId + "<green>.");
                return;
            }
            if (args[2].equalsIgnoreCase("setname")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable chest setname <id> <name>");
                    return;
                }

                String chestId = args[3];
                LootChest lootChest = plugin.getLootChestManager().getLootChest(chestId);

                if (lootChest == null) {
                    MessageUtil.sendMessage(player, "<red>Loot chest with ID <gold>" + chestId + "<red> does not exist.");
                    return;
                }

                // Collect remaining arguments to allow spaces in the name
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 4; i < args.length; i++) {
                    if (i > 4) nameBuilder.append(" ");
                    nameBuilder.append(args[i]);
                }
                String nameString = nameBuilder.toString();

                lootChest.setDisplayName(QTranslatable.fromString(nameString));
                MessageUtil.sendMessage(player, "<green>Set display name of loot chest <gold>" + chestId + "<green> to: <gold>" + nameString);
                return;
            }

            if (args[2].equalsIgnoreCase("setdesc")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable chest setdesc <id> <description>");
                    return;
                }

                String chestId = args[3];
                LootChest lootChest = plugin.getLootChestManager().getLootChest(chestId);

                if (lootChest == null) {
                    MessageUtil.sendMessage(player, "<red>Loot chest with ID <gold>" + chestId + "<red> does not exist.");
                    return;
                }

                // Collect remaining arguments to allow spaces in the description
                StringBuilder descriptionBuilder = new StringBuilder();
                for (int i = 4; i < args.length; i++) {
                    if (i > 4) descriptionBuilder.append(" ");
                    descriptionBuilder.append(args[i]);
                }
                String descriptionString = descriptionBuilder.toString();

                lootChest.setDescription(QTranslatable.fromString(descriptionString));
                MessageUtil.sendMessage(player, "<green>Set description of loot chest <gold>" + chestId + "<green> to: <gold>" + descriptionString);
                return;
            }

            MessageUtil.sendMessage(player, "<red>Unknown subcommand for chest: " + args[2] + " (available: create, delete, setname, setdesc)");
        }
        if (args[1].equalsIgnoreCase("poi")) {
            if (args.length <= 2) {
                MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable poi <create|delete|tp|setname|setdesc> <id> <radius> <set_id> [args]");
                return;
            }
            Location location = player.getLocation();
            if (args[2].equalsIgnoreCase("create")) {
                if (args.length < 6) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable poi create <id> <radius> <set_id>");
                    return;
                }
                String id = args[3];
                double radius;
                try {
                    radius = Double.parseDouble(args[4]);
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(player, "<red>Invalid radius: " + args[4] + ". Must be a number.");
                    return;
                }

                String setId = args[5];
                ExplorationSet set = exploration.getSet(setId);
                if (set == null) {
                    MessageUtil.sendMessage(player, "<red>Exploration set with ID <gold>" + setId + "<red> does not exist.");
                    return;
                }

                PointOfInterest poi = new PointOfInterest(id, location, radius);
                poi.setSet(set);
                set.addExplorable(poi);
                explorationManager.addPointOfInterest(poi);
                explorationManager.save();
                MessageUtil.sendMessage(player, "<green>Created POI <gold>" + id + "<green> at your location with radius <gold>" + radius + "<green> in set <gold>" + setId + "<green>.");
                return;
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
                explorationManager.removePointOfInterest(poi);
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
                return;
            }
            if (args[2].equalsIgnoreCase("setname") || args[2].equalsIgnoreCase("setdisplayname")) {
                if (args.length < 5) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable poi setname <id> <display_name>");
                    return;
                }
                String id = args[3];
                PointOfInterest poi = exploration.getPointOfInterest(id);
                if (poi == null) {
                    MessageUtil.sendMessage(player, "<red>POI with id <gold>" + id + "<red> does not exist.");
                    return;
                }

                // Collect remaining arguments to allow spaces in the display name
                StringBuilder displayNameBuilder = new StringBuilder();
                for (int i = 4; i < args.length; i++) {
                    if (i > 4) displayNameBuilder.append(" ");
                    displayNameBuilder.append(args[i]);
                }
                String displayNameString = displayNameBuilder.toString();

                poi.setDisplayName(QTranslatable.fromString(displayNameString));
                MessageUtil.sendMessage(player, "<green>Set display name of POI <gold>" + id + "<green> to: <gold>" + displayNameString);
                return;
            }
            if (args[2].equalsIgnoreCase("setdesc") || args[2].equalsIgnoreCase("setdescription")) {
                if (args.length < 5) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable poi setdesc <id> <description>");
                    return;
                }
                String id = args[3];
                PointOfInterest poi = exploration.getPointOfInterest(id);
                if (poi == null) {
                    MessageUtil.sendMessage(player, "<red>POI with id <gold>" + id + "<red> does not exist.");
                    return;
                }

                // Collect remaining arguments to allow spaces in the description
                StringBuilder descriptionBuilder = new StringBuilder();
                for (int i = 4; i < args.length; i++) {
                    if (i > 4) descriptionBuilder.append(" ");
                    descriptionBuilder.append(args[i]);
                }
                String descriptionString = descriptionBuilder.toString();

                poi.setFlavourText(QTranslatable.fromString(descriptionString));
                MessageUtil.sendMessage(player, "<green>Set description of POI <gold>" + id + "<green> to: <gold>" + descriptionString);
                return;
            }
            MessageUtil.sendMessage(player, "<red>Unknown subcommand for poi: " + args[2] + " (available: create, delete, teleport, setname, setdesc)");
        }
        if (args[1].equalsIgnoreCase("respawn")) {
            if (args.length <= 2) {
                MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable respawn <create|delete|tp|list|setname|setdesc> <id> [args]");
                return;
            }
            Location location = player.getLocation();
            if (args[2].equalsIgnoreCase("create")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable respawn create <id> [displayName] [unlockMode]");
                    return;
                }
                String id = args[3];
                if (plugin.getRespawnPointManager().getRespawnPoint(id) != null) {
                    MessageUtil.sendMessage(player, "<red>Respawn point with ID <gold>" + id + "<red> already exists.");
                    return;
                }

                String displayName = args.length > 4 ? args[4] : id;
                RespawnPointUnlockMode respawnPointUnlockMode = RespawnPointUnlockMode.NEAR; // Default to NEAR for explorable integration

                if (args.length > 5) {
                    try {
                        respawnPointUnlockMode = RespawnPointUnlockMode.valueOf(args[5].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        MessageUtil.sendMessage(player, "<red>Invalid unlock mode: " + args[5] + ". Valid modes: NEAR, ACTION, QUEST");
                        return;
                    }
                }

                RespawnPoint respawnPoint = new RespawnPoint(id, location);
                respawnPoint.setDisplayName(QTranslatable.fromString(displayName));
                respawnPoint.setUnlockMode(respawnPointUnlockMode);

                plugin.getRespawnPointManager().addRespawnPoint(respawnPoint);
                plugin.getRespawnPointManager().save();

                MessageUtil.sendMessage(player, "<green>Created respawn point <gold>" + id + "<green> at your location with unlock mode <gold>" + respawnPointUnlockMode + "<green>.");

                if (respawnPointUnlockMode == RespawnPointUnlockMode.NEAR) {
                    MessageUtil.sendMessage(player, "<yellow>Added to exploration system as an explorable respawn point.");
                }
                return;
            }
            if (args[2].equalsIgnoreCase("delete")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable respawn delete <id>");
                    return;
                }
                String id = args[3];
                RespawnPoint respawnPoint = plugin.getRespawnPointManager().getRespawnPoint(id);
                if (respawnPoint == null) {
                    MessageUtil.sendMessage(player, "<red>Respawn point with id <gold>" + id + "<red> does not exist.");
                    return;
                }
                plugin.getRespawnPointManager().removeRespawnPoint(respawnPoint);
                plugin.getRespawnPointManager().save();
                MessageUtil.sendMessage(player, "<green>Deleted respawn point <gold>" + id + "<green>.");
                return;
            }
            if (args[2].equalsIgnoreCase("tp") || args[2].equalsIgnoreCase("teleport")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable respawn teleport <id>");
                    return;
                }
                String id = args[3];
                RespawnPoint respawnPoint = plugin.getRespawnPointManager().getRespawnPoint(id);
                if (respawnPoint == null) {
                    MessageUtil.sendMessage(player, "<red>Respawn point with id <gold>" + id + "<red> does not exist.");
                    return;
                }
                Location respawnLocation = respawnPoint.getLocation();
                if (respawnLocation == null) {
                    MessageUtil.sendMessage(player, "<red>Respawn point with id <gold>" + id + "<red> has no valid location.");
                    return;
                }
                if (respawnLocation.getWorld() == null) {
                    MessageUtil.sendMessage(player, "<red>Respawn point with id <gold>" + id + "<red> has no valid world.");
                    return;
                }
                player.teleportAsync(respawnLocation);
                MessageUtil.sendMessage(player, "<green>Teleported to respawn point <gold>" + id + "<green>.");
                return;
            }
            if (args[2].equalsIgnoreCase("list")) {
                StringBuilder sb = new StringBuilder("<green>Respawn points: ");
                for (RespawnPoint point : plugin.getRespawnPointManager().getRespawnPoints()) {
                    sb.append("<gold>").append(point.getId()).append(" (").append(point.getUnlockMode()).append(")<green>, ");
                }
                MessageUtil.sendMessage(player, sb.toString());
                return;
            }
            if (args[2].equalsIgnoreCase("setname")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable respawn setname <id> <name>");
                    return;
                }
                String id = args[3];
                RespawnPoint respawnPoint = plugin.getRespawnPointManager().getRespawnPoint(id);
                if (respawnPoint == null) {
                    MessageUtil.sendMessage(player, "<red>Respawn point with id <gold>" + id + "<red> does not exist.");
                    return;
                }

                // Collect remaining arguments to allow spaces in the name
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 4; i < args.length; i++) {
                    if (i > 4) nameBuilder.append(" ");
                    nameBuilder.append(args[i]);
                }
                String nameString = nameBuilder.toString();

                respawnPoint.setDisplayName(QTranslatable.fromString(nameString));
                MessageUtil.sendMessage(player, "<green>Set display name of respawn point <gold>" + id + "<green> to: <gold>" + nameString);
                return;
            }
            if (args[2].equalsIgnoreCase("setdesc")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(player, "<red>Usage: /qxl explorable respawn setdesc <id> <description>");
                    return;
                }
                String id = args[3];
                RespawnPoint respawnPoint = plugin.getRespawnPointManager().getRespawnPoint(id);
                if (respawnPoint == null) {
                    MessageUtil.sendMessage(player, "<red>Respawn point with id <gold>" + id + "<red> does not exist.");
                    return;
                }

                // Collect remaining arguments to allow spaces in the description
                StringBuilder descriptionBuilder = new StringBuilder();
                for (int i = 4; i < args.length; i++) {
                    if (i > 4) descriptionBuilder.append(" ");
                    descriptionBuilder.append(args[i]);
                }
                String descriptionString = descriptionBuilder.toString();

                respawnPoint.setDescription(QTranslatable.fromString(descriptionString));
                MessageUtil.sendMessage(player, "<green>Set description of respawn point <gold>" + id + "<green> to: <gold>" + descriptionString);
                return;
            }
            MessageUtil.sendMessage(player, "<red>Unknown subcommand for respawn: " + args[2] + " (available: create, delete, teleport, list, setname, setdesc)");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completes = new java.util.ArrayList<>(List.of("set", "poi", "chest", "respawn"));
            completes.removeIf(id -> !id.startsWith(args[1]));
            return completes;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
            List<String> completes = new java.util.ArrayList<>(List.of("create", "delete", "add", "parent", "list", "setname", "setdesc"));
            completes.removeIf(id -> !id.startsWith(args[2]));
            return completes;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("poi")) {
            List<String> completes = new java.util.ArrayList<>(List.of("create", "delete", "tp", "teleport", "setname", "setdisplayname", "setdesc", "setdescription"));
            completes.removeIf(id -> !id.startsWith(args[2]));
            return completes;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("chest")) {
            List<String> completes = new java.util.ArrayList<>(List.of("create", "delete", "setname", "setdesc"));
            completes.removeIf(id -> !id.startsWith(args[2]));
            return completes;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("respawn")) {
            List<String> completes = new java.util.ArrayList<>(List.of("create", "delete", "tp", "teleport", "list", "setname", "setdesc"));
            completes.removeIf(id -> !id.startsWith(args[2]));
            return completes;
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("add")) {
            List<String> completes = new java.util.ArrayList<>(List.of("poi", "chest", "event", "respawn"));
            completes.removeIf(id -> !id.startsWith(args[3]));
            return completes;
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("add") && args[3].equalsIgnoreCase("poi")) {
            return exploration.getPointOfInterestIDs().stream()
                    .filter(id -> id.startsWith(args[4]))
                    .toList();
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("add") && args[3].equalsIgnoreCase("respawn")) {
            return exploration.getStandaloneExplorables().stream()
                    .filter(explorable -> explorable instanceof RespawnPoint)
                    .map(Explorable::id)
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
        if (args.length == 4 && args[1].equalsIgnoreCase("respawn") && (args[2].equalsIgnoreCase("tp") || args[2].equalsIgnoreCase("teleport") || args[2].equalsIgnoreCase("delete"))) {
            return plugin.getRespawnPointManager().getRespawnPoints().stream()
                    .map(RespawnPoint::getId)
                    .filter(id -> id.startsWith(args[3]))
                    .toList();
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("poi") && (args[2].equalsIgnoreCase("setname") || args[2].equalsIgnoreCase("setdisplayname") || args[2].equalsIgnoreCase("setdesc") || args[2].equalsIgnoreCase("setdescription"))) {
            return exploration.getPointOfInterestIDs().stream()
                    .filter(id -> id.startsWith(args[3]))
                    .toList();
        }
        if (args.length == 6 && args[1].equalsIgnoreCase("respawn") && args[2].equalsIgnoreCase("create")) {
            List<String> completes = new java.util.ArrayList<>(List.of("NEAR", "ACTION", "QUEST"));
            completes.removeIf(id -> !id.startsWith(args[5].toUpperCase()));
            return completes;
        }
        if (args.length == 6 && args[1].equalsIgnoreCase("poi") && args[2].equalsIgnoreCase("create")) {
            return exploration.getSetIDs().stream()
                    .filter(id -> id.startsWith(args[5]))
                    .toList();
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("chest") && args[2].equalsIgnoreCase("create")) {
            return exploration.getSetIDs().stream()
                    .filter(id -> id.startsWith(args[4]))
                    .toList();
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("set") && (args[2].equalsIgnoreCase("setname") || args[2].equalsIgnoreCase("setdesc"))) {
            return exploration.getSetIDs().stream()
                    .filter(id -> id.startsWith(args[3]))
                    .toList();
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("chest") && (args[2].equalsIgnoreCase("setname") || args[2].equalsIgnoreCase("setdesc") || args[2].equalsIgnoreCase("delete"))) {
            return plugin.getLootChestManager().getAllLootChests().values().stream()
                    .map(LootChest::id)
                    .filter(id -> id.startsWith(args[3]))
                    .toList();
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("respawn") && (args[2].equalsIgnoreCase("setname") || args[2].equalsIgnoreCase("setdesc"))) {
            return plugin.getRespawnPointManager().getRespawnPoints().stream()
                    .map(RespawnPoint::getId)
                    .filter(id -> id.startsWith(args[3]))
                    .toList();
        }
        return super.onTabComplete(sender, args);
    }
}
