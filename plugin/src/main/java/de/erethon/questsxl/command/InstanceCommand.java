package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.instancing.InstanceManager;
import de.erethon.questsxl.instancing.InstanceTemplate;
import de.erethon.questsxl.instancing.InstancedArea;
import de.erethon.questsxl.instancing.apartment.ApartmentService;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Debug command for testing the instancing system.
 * Usage:
 * /qxl instance pos1 - Sets first corner of selection
 * /qxl instance pos2 - Sets second corner of selection
 * /qxl instance create <id> - Creates a template from selection
 * /qxl instance enter <templateId> - Enters an instance
 * /qxl instance leave - Leaves current instance
 * /qxl instance reset - Resets current instance to template state
 * /qxl instance save <templateId> - Saves a template to database
 * /qxl instance load <templateId> - Loads a template from database
 * /qxl instance list - Lists all templates
 * /qxl instance info - Shows info about current instance
 */
public class InstanceCommand extends ECommand {

    private final QuestsXL plugin = QuestsXL.get();

    public InstanceCommand() {
        setCommand("instance");
        setAliases("inst", "i");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("/qxl instance <pos1|pos2|create|enter|leave|reset|save|load|list|info|sign>");
        setPermission("qxl.admin.instance");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        InstanceManager manager = plugin.getInstanceManager();

        if (manager == null) {
            MessageUtil.sendMessage(commandSender, QuestsXL.ERROR + "Instance manager not initialized.");
            return;
        }

        if (args.length < 2) {
            MessageUtil.sendMessage(commandSender, QuestsXL.ERROR + "Usage: /qxl instance <pos1|pos2|create|enter|leave|reset|save|load|list|info>");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "pos1" -> handlePos1(player, manager);
            case "pos2" -> handlePos2(player, manager);
            case "create" -> handleCreate(args, player, manager);
            case "enter" -> handleEnter(args, player, manager);
            case "leave" -> handleLeave(player, manager);
            case "reset" -> handleReset(player, manager);
            case "save" -> handleSave(args, player, manager);
            case "load" -> handleLoad(args, player, manager);
            case "list" -> handleList(player, manager);
            case "info" -> handleInfo(player, manager);
            case "setblock" -> handleSetBlock(args, player, manager);
            case "sign" -> handleSign(args, player, manager);
            case "rentable" -> handleRentable(args, player, manager);
            default -> MessageUtil.sendMessage(commandSender, QuestsXL.ERROR + "Unknown action: " + action);
        }
    }

    private void handlePos1(Player player, InstanceManager manager) {
        Location loc = player.getLocation().getBlock().getLocation();
        manager.setPos1(player, loc);
        MessageUtil.sendMessage(player, "&aPos1 set to &e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

        Location pos2 = manager.getPos2(player);
        if (pos2 != null) {
            showSelectionInfo(player, loc, pos2);
        }
    }

    private void handlePos2(Player player, InstanceManager manager) {
        Location loc = player.getLocation().getBlock().getLocation();
        manager.setPos2(player, loc);
        MessageUtil.sendMessage(player, "&aPos2 set to &e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

        Location pos1 = manager.getPos1(player);
        if (pos1 != null) {
            showSelectionInfo(player, pos1, loc);
        }
    }

    private void showSelectionInfo(Player player, Location pos1, Location pos2) {
        int sizeX = Math.abs(pos2.getBlockX() - pos1.getBlockX()) + 1;
        int sizeY = Math.abs(pos2.getBlockY() - pos1.getBlockY()) + 1;
        int sizeZ = Math.abs(pos2.getBlockZ() - pos1.getBlockZ()) + 1;
        int totalBlocks = sizeX * sizeY * sizeZ;
        MessageUtil.sendMessage(player, "&7Selection size: &e" + sizeX + "x" + sizeY + "x" + sizeZ +
                               " &7(&e" + totalBlocks + " &7blocks)");
    }

    private void handleCreate(String[] args, Player player, InstanceManager manager) {
        if (args.length < 3) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Usage: /qxl instance create <id>");
            return;
        }

        String templateId = args[2];

        if (manager.getTemplate(templateId) != null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Template '" + templateId + "' already exists.");
            return;
        }

        Location pos1 = manager.getPos1(player);
        Location pos2 = manager.getPos2(player);

        if (pos1 == null || pos2 == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "You must set both pos1 and pos2 first!");
            MessageUtil.sendMessage(player, "&7Use &e/qxl instance pos1 &7and &e/qxl instance pos2");
            return;
        }

        if (!pos1.getWorld().equals(pos2.getWorld())) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Both positions must be in the same world!");
            return;
        }

        BoundingBox bounds = new BoundingBox(
                Math.min(pos1.getBlockX(), pos2.getBlockX()),
                Math.min(pos1.getBlockY(), pos2.getBlockY()),
                Math.min(pos1.getBlockZ(), pos2.getBlockZ()),
                Math.max(pos1.getBlockX(), pos2.getBlockX()),
                Math.max(pos1.getBlockY(), pos2.getBlockY()),
                Math.max(pos1.getBlockZ(), pos2.getBlockZ())
        );

        try {
            InstanceTemplate template = manager.createTemplate(templateId, pos1.getWorld(), bounds);
            MessageUtil.sendMessage(player, "&aCreated template '&e" + templateId + "&a' with " +
                    template.getBaseBlocks().size() + " blocks captured.");
            MessageUtil.sendMessage(player, "&7Template will be auto-saved to database.");

            // Clear selections after successful creation
            manager.clearSelections(player);
        } catch (Exception e) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Failed to create template: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleEnter(String[] args, Player player, InstanceManager manager) {
        if (args.length < 3) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Usage: /qxl instance enter <templateId>");
            return;
        }

        String templateId = args[2];
        InstanceTemplate template = manager.getTemplate(templateId);

        if (template == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Template '" + templateId + "' not found.");
            return;
        }

        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        if (qPlayer == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Player data not loaded.");
            return;
        }

        try {
            InstancedArea instance = manager.enterInstance(qPlayer, templateId);
            MessageUtil.sendMessage(player, "&aEntered instance '&e" + instance.getId() + "&a'.");
        } catch (Exception e) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Failed to enter instance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleLeave(Player player, InstanceManager manager) {
        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        if (qPlayer == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Player data not loaded.");
            return;
        }

        InstancedArea instance = manager.getActiveInstance(qPlayer);
        if (instance == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "You are not in an instance.");
            return;
        }

        manager.leaveInstance(qPlayer);
        MessageUtil.sendMessage(player, "&aLeft instance '&e" + instance.getId() + "&a'.");
    }

    private void handleReset(Player player, InstanceManager manager) {
        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        if (qPlayer == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Player data not loaded.");
            return;
        }

        InstancedArea instance = manager.getActiveInstance(qPlayer);
        if (instance == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "You are not in an instance.");
            return;
        }

        manager.resetInstance(instance);
        MessageUtil.sendMessage(player, "&aReset instance '&e" + instance.getId() + "&a' to template state.");
    }

    private void handleSave(String[] args, Player player, InstanceManager manager) {
        if (args.length < 3) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Usage: /qxl instance save <templateId>");
            return;
        }

        String templateId = args[2];
        InstanceTemplate template = manager.getTemplate(templateId);

        if (template == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Template '" + templateId + "' not found.");
            return;
        }

        MessageUtil.sendMessage(player, "&7Saving template '&e" + templateId + "&7'...");
        manager.saveTemplate(template).thenRun(() -> {
            MessageUtil.sendMessage(player, "&aTemplate '&e" + templateId + "&a' saved to database.");
        }).exceptionally(e -> {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Failed to save template: " + e.getMessage());
            return null;
        });
    }

    private void handleLoad(String[] args, Player player, InstanceManager manager) {
        if (args.length < 3) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Usage: /qxl instance load <templateId>");
            return;
        }

        String templateId = args[2];

        MessageUtil.sendMessage(player, "&7Loading template '&e" + templateId + "&7'...");
        manager.loadTemplate(templateId).thenAccept(template -> {
            if (template != null) {
                MessageUtil.sendMessage(player, "&aTemplate '&e" + templateId + "&a' loaded with " +
                        template.getBaseBlocks().size() + " blocks.");
            } else {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Template '" + templateId + "' not found in database.");
            }
        }).exceptionally(e -> {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Failed to load template: " + e.getMessage());
            return null;
        });
    }

    private void handleList(Player player, InstanceManager manager) {
        var templates = manager.getTemplates();
        var instances = manager.getActiveInstances();

        MessageUtil.sendMessage(player, "&8&m               &r &aInstance System &8&m               &r");
        MessageUtil.sendMessage(player, "&7Templates&8: &a" + templates.size());
        for (var template : templates) {
            String rentableStatus = template.isRentable() ? " &a[rentable]" : "";
            MessageUtil.sendMessage(player, "  &8- &e" + template.getId() + " &7(" + template.getBaseBlocks().size() + " blocks)" + rentableStatus);
        }

        MessageUtil.sendMessage(player, "&7Active Instances&8: &a" + instances.size());
        for (var instance : instances) {
            MessageUtil.sendMessage(player, "  &8- &e" + instance.getId() + " &7(" + instance.getParticipants().size() + " players)");
        }
    }

    private void handleInfo(Player player, InstanceManager manager) {
        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        if (qPlayer == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Player data not loaded.");
            return;
        }

        InstancedArea instance = manager.getActiveInstance(qPlayer);
        if (instance == null) {
            MessageUtil.sendMessage(player, "&7You are not in an instance.");
            return;
        }

        MessageUtil.sendMessage(player, "&8&m               &r &aInstance Info &8&m               &r");
        MessageUtil.sendMessage(player, "&7Instance ID&8: &e" + instance.getId());
        MessageUtil.sendMessage(player, "&7Template&8: &e" + instance.getTemplate().getId());
        MessageUtil.sendMessage(player, "&7Template Rentable&8: &" + (instance.getTemplate().isRentable() ? "a" : "c") + instance.getTemplate().isRentable());
        MessageUtil.sendMessage(player, "&7Participants&8: &a" + instance.getParticipants().size());
        MessageUtil.sendMessage(player, "&7Modified Blocks&8: &a" + instance.getModifiedBlocks().size());
        MessageUtil.sendMessage(player, "&7Virtual Block Entities&8: &a" + instance.getBlockEntities().size());
        MessageUtil.sendMessage(player, "&7Dirty&8: &a" + instance.isDirty());

        if (instance.getOwnerCharacterId() != null) {
            MessageUtil.sendMessage(player, "&7Owner Character&8: &e" + instance.getOwnerCharacterId().toString().substring(0, 8) + "...");
        }
    }

    private void handleSetBlock(String[] args, Player player, InstanceManager manager) {
        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        if (qPlayer == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Player data not loaded.");
            return;
        }

        InstancedArea instance = manager.getActiveInstance(qPlayer);
        if (instance == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "You are not in an instance.");
            return;
        }

        // Set the block the player is looking at to air (for testing)
        var targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "No block in range.");
            return;
        }

        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(
                targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());
        net.minecraft.world.level.block.state.BlockState air =
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

        manager.setBlock(instance, pos, air);
        MessageUtil.sendMessage(player, "&aSet block at " + targetBlock.getX() + ", " +
                targetBlock.getY() + ", " + targetBlock.getZ() + " to air in instance.");
    }

    private void handleSign(String[] args, Player player, InstanceManager manager) {
        if (args.length < 3) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Usage: /qxl instance sign <templateId> (look at a sign)");
            return;
        }

        String templateId = args[2];
        InstanceTemplate template = manager.getTemplate(templateId);
        if (template == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Template '" + templateId + "' not found.");
            return;
        }

        ApartmentService apartmentService = plugin.getApartmentService();
        if (apartmentService == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Apartment service not initialized.");
            return;
        }

        var targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof org.bukkit.block.Sign sign)) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Look at a sign to link it.");
            return;
        }

        apartmentService.tagSign(sign, templateId);
        MessageUtil.sendMessage(player, "&aLinked sign to apartment template '&e" + templateId + "&a'.");
    }

    private void handleRentable(String[] args, Player player, InstanceManager manager) {
        if (args.length < 4) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Usage: /qxl instance rentable <templateId> <true|false>");
            return;
        }

        String templateId = args[2];
        InstanceTemplate template = manager.getTemplate(templateId);
        if (template == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Template '" + templateId + "' not found.");
            return;
        }

        boolean rentable = Boolean.parseBoolean(args[3]);
        template.setRentable(rentable);

        MessageUtil.sendMessage(player, "&aTemplate '&e" + templateId + "&a' is now " +
                (rentable ? "&arentable" : "&cnot rentable") + "&a.");

        if (rentable) {
            MessageUtil.sendMessage(player, "&7Players can now rent this template via signs or proximity.");
        } else {
            MessageUtil.sendMessage(player, "&7This template will not appear in apartment rental searches.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> actions = new ArrayList<>(List.of("pos1", "pos2", "create", "enter", "leave", "reset", "save", "load", "list", "info", "setblock", "sign", "rentable"));
            actions.removeIf(a -> !a.startsWith(args[1].toLowerCase()));
            return actions;
        }

        if (args.length == 3) {
            String action = args[1].toLowerCase();
            InstanceManager manager = plugin.getInstanceManager();

            if (manager == null) {
                return List.of();
            }

            if (action.equals("enter") || action.equals("save") || action.equals("load") || action.equals("sign") || action.equals("rentable")) {
                List<String> templateIds = new ArrayList<>();
                for (var template : manager.getTemplates()) {
                    templateIds.add(template.getId());
                }
                templateIds.removeIf(id -> !id.startsWith(args[2]));
                return templateIds;
            }

            if (action.equals("create")) {
                return List.of("<id>");
            }
        }

        if (args.length == 4) {
            String action = args[1].toLowerCase();

            if (action.equals("rentable")) {
                List<String> options = new ArrayList<>(List.of("true", "false"));
                options.removeIf(o -> !o.startsWith(args[3].toLowerCase()));
                return options;
            }
        }

        return List.of();
    }
}

