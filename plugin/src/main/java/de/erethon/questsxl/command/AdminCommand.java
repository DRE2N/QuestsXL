package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.CompletedExplorable;
import de.erethon.questsxl.livingworld.Explorable;
import de.erethon.questsxl.livingworld.ExplorationSet;
import de.erethon.questsxl.respawn.RespawnPoint;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.common.QStage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommand extends ECommand {

    QuestsXL plugin = QuestsXL.get();

    public AdminCommand() {
        setCommand("admin");
        setAliases("a");
        setMinArgs(0);
        setMaxArgs(5);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.admin.admincommand");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args.length < 2) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Unbekannter Befehl. Probiere z.B: /q admin info oder /q admin list");
            return;
        }
        if (args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("i")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe eine Quest an.");
                return;
            }
            QQuest quest = plugin.getQuestManager().getByName(args[2]);
            if (quest == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Quest existiert nicht.");
                return;
            }
            MessageUtil.sendMessage(player, "&7ID: &6" + quest.getName());
            MessageUtil.sendMessage(player, "&7Name: &6" + quest.displayName().getAsString());
            MessageUtil.sendMessage(player, "&7Description: &6" + quest.getDescription());
            MessageUtil.sendMessage(player, "&7Stages (" + quest.getStages().size() + "):");
            for (QStage stage : quest.getStages()) {
                MessageUtil.sendMessage(player, "&8- [&6" + stage.getId() + "&8] &7" + stage.getDescription());
            }
            return;
        }
        if (args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase("ls")) {
            if (args.length < 3) {
                for (QQuest quest : plugin.getQuestManager().getQuests()) {
                    MessageUtil.sendMessage(player, "&8- &6" + quest.getName());
                }
                return;
            }
            Player otherPlayer = Bukkit.getPlayer(args[2]);
            if (otherPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Dieser Spieler existiert nicht");
                return;
            }
            QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(otherPlayer);
            if (qPlayer.getActiveQuests().isEmpty()) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Dieser hat keine aktiven Quests.");
                return;
            }
            for (ActiveQuest active : qPlayer.getActiveQuests().keySet()) {
                MessageUtil.sendMessage(player, "&8- &6" + active.getQuest().getName() + ": &8[&b" + active.getCurrentStage().getId() + "&8] " + active.getCurrentStage().getDescription());
            }
            return;
        }
        if (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("g")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe einen Spieler an.");
                return;
            }
            Player otherPlayer = Bukkit.getPlayer(args[2]);
            if (otherPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Dieser Spieler existiert nicht");
                return;
            }
            QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(otherPlayer);
            if (args.length < 4) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe eine Quest an.");
                return;
            }
            QQuest quest = plugin.getQuestManager().getByName(args[3]);
            if (quest == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Quest existiert nicht.");
                return;
            }
            qPlayer.addActive(quest);
            qPlayer.setTrackedQuest(quest, 9999);
            MessageUtil.sendMessage(otherPlayer, "&a" + player.getName() + " &7hat dir die Quest &a" + quest.getName() + " &7gegeben.");
            MessageUtil.sendMessage(player, "&a" + player.getName() + " &7hat erfolgreich die Quest &a" + quest.getName() + " &7gestartet.");
            return;
        }
        if (args[1].equalsIgnoreCase("objectives") || args[1].equalsIgnoreCase("o")) {
            QPlayer player1 = plugin.getDatabaseManager().getCurrentPlayer(player);
            MessageUtil.sendMessage(player, "&7Aktive Objectives:");
            player1.getCurrentObjectives().forEach((objective) -> {
                MessageUtil.sendMessage(player, "&8- &6" + objective.getObjective().getClass().toString());
            });
            return;
        }
        if (args[1].equalsIgnoreCase("explore") || args[1].equalsIgnoreCase("ex")) {
            if (args.length < 4) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Usage: /q admin explore <player> <set_id> [explorable_id]");
                return;
            }
            Player targetPlayer = Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Spieler nicht gefunden.");
                return;
            }
            QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(targetPlayer);
            if (qPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "QPlayer nicht gefunden.");
                return;
            }

            String setId = args[3];
            ExplorationSet explorationSet = plugin.getExploration().getSet(setId);
            if (explorationSet == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Exploration Set '" + setId + "' nicht gefunden.");
                return;
            }

            if (args.length >= 5) {
                // Specific explorable
                String explorableId = args[4];
                Explorable explorable = explorationSet.getExplorable(explorableId);
                if (explorable == null) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Explorable '" + explorableId + "' nicht in Set '" + setId + "' gefunden.");
                    return;
                }

                boolean success = qPlayer.getExplorer().completeExplorable(explorationSet, explorable, System.currentTimeMillis());
                if (success) {
                    MessageUtil.sendMessage(player, "&7Spieler &6" + targetPlayer.getName() + " &7hat jetzt &6" + explorable.displayName().getAsString() + " &7erkundet.");
                    MessageUtil.sendMessage(targetPlayer, "&7Ein Admin hat dir &6" + explorable.displayName().getAsString() + " &7als erkundet markiert.");
                } else {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Spieler hat bereits &6" + explorable.displayName().getAsString() + " &7erkundet.");
                }
            } else {
                // All explorables in set
                int completed = 0;
                for (Explorable explorable : explorationSet.entries()) {
                    if (qPlayer.getExplorer().completeExplorable(explorationSet, explorable, System.currentTimeMillis())) {
                        completed++;
                    }
                }
                MessageUtil.sendMessage(player, "&7Spieler &6" + targetPlayer.getName() + " &7hat jetzt &6" + completed + " &7neue Explorables in Set '" + setId + "' erkundet.");
                if (completed > 0) {
                    MessageUtil.sendMessage(targetPlayer, "&7Ein Admin hat dir &6" + completed + " &7Explorables in Set '" + setId + "' als erkundet markiert.");
                }
            }
            return;
        }
        if (args[1].equalsIgnoreCase("unexplore") || args[1].equalsIgnoreCase("unex")) {
            if (args.length < 4) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Usage: /q admin unexplore <player> <set_id> [explorable_id]");
                return;
            }
            Player targetPlayer = Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Spieler nicht gefunden.");
                return;
            }
            QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(targetPlayer);
            if (qPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "QPlayer nicht gefunden.");
                return;
            }

            String setId = args[3];
            ExplorationSet explorationSet = plugin.getExploration().getSet(setId);
            if (explorationSet == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Exploration Set '" + setId + "' nicht gefunden.");
                return;
            }

            if (args.length >= 5) {
                // Specific explorable
                String explorableId = args[4];
                Explorable explorable = explorationSet.getExplorable(explorableId);
                if (explorable == null) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Explorable '" + explorableId + "' nicht in Set '" + setId + "' gefunden.");
                    return;
                }

                boolean success = removeExplorableFromPlayer(qPlayer, explorationSet, explorable);
                if (success) {
                    qPlayer.saveToDatabase();
                    MessageUtil.sendMessage(player, "&7Spieler &6" + targetPlayer.getName() + " &7hat &6" + explorable.displayName().getAsString() + " &7als unerkundet markiert.");
                    MessageUtil.sendMessage(targetPlayer, "&7Ein Admin hat &6" + explorable.displayName().getAsString() + " &7als unerkundet markiert.");
                } else {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Spieler hatte &6" + explorable.displayName().getAsString() + " &7noch nicht erkundet.");
                }
            } else {
                int removed = removeAllExplorablesFromSet(qPlayer, explorationSet);
                // Save to database immediately to ensure persistence
                if (removed > 0) {
                    qPlayer.saveToDatabase();
                }
                MessageUtil.sendMessage(player, "&7Spieler &6" + targetPlayer.getName() + " &7hat &6" + removed + " &7Explorables in Set '" + setId + "' als unerkundet markiert.");
                if (removed > 0) {
                    MessageUtil.sendMessage(targetPlayer, "&7Ein Admin hat &6" + removed + " &7Explorables in Set '" + setId + "' als unerkundet markiert.");
                }
            }
            return;
        }
        if (args[1].equalsIgnoreCase("unlock") || args[1].equalsIgnoreCase("ul")) {
            if (args.length < 4) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Usage: /q admin unlock <player> <respawn_id>");
                return;
            }
            Player targetPlayer = Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Spieler nicht gefunden.");
                return;
            }
            QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(targetPlayer);
            if (qPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "QPlayer nicht gefunden.");
                return;
            }

            String respawnId = args[3];
            RespawnPoint respawnPoint = plugin.getRespawnPointManager().getRespawnPoint(respawnId);
            if (respawnPoint == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Respawn Point '" + respawnId + "' nicht gefunden.");
                return;
            }

            // Handle unlocking based on whether it's part of an exploration set or standalone
            ExplorationSet set = plugin.getExploration().getSetContaining(respawnPoint);
            boolean success;

            if (set != null) {
                // Part of an exploration set - unlock through the exploration system
                success = qPlayer.getExplorer().completeExplorable(set, respawnPoint, System.currentTimeMillis());
            } else {
                // Standalone explorable - unlock directly
                success = qPlayer.getExplorer().completeStandaloneExplorable(respawnPoint, System.currentTimeMillis());
            }

            if (success) {
                // Save to database immediately to ensure persistence
                qPlayer.saveToDatabase();
                MessageUtil.sendMessage(player, "&7Spieler &6" + targetPlayer.getName() + " &7hat jetzt Respawn Point &6" + respawnPoint.displayName().getAsString() + " &7freigeschaltet.");
                MessageUtil.sendMessage(targetPlayer, "&7Ein Admin hat dir Respawn Point &6" + respawnPoint.displayName().getAsString() + " &7freigeschaltet.");
            } else {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Spieler hat bereits Respawn Point &6" + respawnPoint.displayName().getAsString() + " &7freigeschaltet.");
            }
            return;
        }
        if (args[1].equalsIgnoreCase("lock") || args[1].equalsIgnoreCase("lk")) {
            if (args.length < 4) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Usage: /q admin lock <player> <respawn_id>");
                return;
            }
            Player targetPlayer = Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Spieler nicht gefunden.");
                return;
            }
            QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(targetPlayer);
            if (qPlayer == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "QPlayer nicht gefunden.");
                return;
            }

            String respawnId = args[3];
            RespawnPoint respawnPoint = plugin.getRespawnPointManager().getRespawnPoint(respawnId);
            if (respawnPoint == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Respawn Point '" + respawnId + "' nicht gefunden.");
                return;
            }

            // Handle locking based on whether it's part of an exploration set or standalone
            ExplorationSet set = plugin.getExploration().getSetContaining(respawnPoint);
            boolean success;

            if (set != null) {
                // Part of an exploration set - remove through the exploration system
                success = removeExplorableFromPlayer(qPlayer, set, respawnPoint);
            } else {
                // Standalone explorable - remove directly
                success = removeStandaloneExplorableFromPlayer(qPlayer, respawnPoint);
            }

            if (success) {
                // Save to database immediately to ensure persistence
                qPlayer.saveToDatabase();
                MessageUtil.sendMessage(player, "&7Spieler &6" + targetPlayer.getName() + " &7hat Respawn Point &6" + respawnPoint.displayName().getAsString() + " &7gesperrt.");
                MessageUtil.sendMessage(targetPlayer, "&7Ein Admin hat Respawn Point &6" + respawnPoint.displayName().getAsString() + " &7gesperrt.");
            } else {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Spieler hatte Respawn Point &6" + respawnPoint.displayName().getAsString() + " &7noch nicht freigeschaltet.");
            }
            return;
        }
        MessageUtil.sendMessage(player, QuestsXL.ERROR + "Unbekannter Befehl. Probiere z.B: /q admin info, /q admin objectives, /q admin list, /q admin give, /q admin explore, /q admin unexplore, /q admin unlock oder /q admin lock");
    }

    /**
     * Removes a specific explorable from a player's completed explorables
     */
    private boolean removeExplorableFromPlayer(QPlayer qPlayer, ExplorationSet set, Explorable explorable) {
        Set<CompletedExplorable> completed = qPlayer.getExplorer().getCompletedExplorables().get(set);
        if (completed == null) {
            return false;
        }

        Iterator<CompletedExplorable> iterator = completed.iterator();
        while (iterator.hasNext()) {
            CompletedExplorable completedExplorable = iterator.next();
            if (completedExplorable.explorable().equals(explorable)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Removes all explorables from a specific set for a player
     */
    private int removeAllExplorablesFromSet(QPlayer qPlayer, ExplorationSet set) {
        Set<CompletedExplorable> completed = qPlayer.getExplorer().getCompletedExplorables().get(set);
        if (completed == null) {
            return 0;
        }

        int removed = completed.size();
        completed.clear();
        return removed;
    }

    /**
     * Removes a standalone explorable from a player's completed standalone explorables
     */
    private boolean removeStandaloneExplorableFromPlayer(QPlayer qPlayer, Explorable explorable) {
        Set<CompletedExplorable> completed = qPlayer.getExplorer().getCompletedStandaloneExplorables();
        if (completed == null) {
            return false;
        }

        Iterator<CompletedExplorable> iterator = completed.iterator();
        while (iterator.hasNext()) {
            CompletedExplorable completedExplorable = iterator.next();
            if (completedExplorable.explorable().id().equals(explorable.id())) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completes = new java.util.ArrayList<>(List.of("info", "i", "list", "ls", "give", "g", "objectives", "o", "explore", "ex", "unexplore", "unex", "unlock", "ul", "lock", "lk"));
            completes.removeIf(id -> !id.toLowerCase().startsWith(args[1].toLowerCase()));
            return completes;
        }

        // For commands that need player names
        if (args.length == 3 && (args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase("ls") ||
                args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("g") ||
                args[1].equalsIgnoreCase("explore") || args[1].equalsIgnoreCase("ex") ||
                args[1].equalsIgnoreCase("unexplore") || args[1].equalsIgnoreCase("unex") ||
                args[1].equalsIgnoreCase("unlock") || args[1].equalsIgnoreCase("ul") ||
                args[1].equalsIgnoreCase("lock") || args[1].equalsIgnoreCase("lk"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // For quest info command
        if (args.length == 3 && (args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("i"))) {
            return plugin.getQuestManager().getQuests().stream()
                    .map(QQuest::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // For give command - quest names
        if (args.length == 4 && (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("g"))) {
            return plugin.getQuestManager().getQuests().stream()
                    .map(QQuest::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // For explore/unexplore commands - exploration set IDs
        if (args.length == 4 && (args[1].equalsIgnoreCase("explore") || args[1].equalsIgnoreCase("ex") ||
                args[1].equalsIgnoreCase("unexplore") || args[1].equalsIgnoreCase("unex"))) {
            return plugin.getExploration().getSetIDs().stream()
                    .filter(id -> id.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // For explore/unexplore commands - specific explorable IDs within a set
        if (args.length == 5 && (args[1].equalsIgnoreCase("explore") || args[1].equalsIgnoreCase("ex") ||
                args[1].equalsIgnoreCase("unexplore") || args[1].equalsIgnoreCase("unex"))) {
            String setId = args[3];
            ExplorationSet explorationSet = plugin.getExploration().getSet(setId);
            if (explorationSet != null) {
                return explorationSet.entries().stream()
                        .map(explorable -> explorable.id())
                        .filter(id -> id.toLowerCase().startsWith(args[4].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        // For unlock/lock commands - all respawn point IDs (both explorable and standalone)
        if (args.length == 4 && (args[1].equalsIgnoreCase("unlock") || args[1].equalsIgnoreCase("ul") ||
                args[1].equalsIgnoreCase("lock") || args[1].equalsIgnoreCase("lk"))) {
            List<String> allRespawnIds = new java.util.ArrayList<>();

            // Add all respawn point IDs from RespawnPointManager
            allRespawnIds.addAll(plugin.getRespawnPointManager().getRespawnPoints().stream()
                    .map(RespawnPoint::getId)
                    .collect(Collectors.toList()));

            // Remove duplicates and filter by input
            return allRespawnIds.stream()
                    .distinct()
                    .filter(id -> id.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return super.onTabComplete(sender, args);
    }
}
