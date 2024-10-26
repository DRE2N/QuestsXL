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

import java.util.stream.Collectors;

public class RegionCommand extends ECommand {

    QuestsXL plugin = QuestsXL.getInstance();
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
            if (region.getLinkedQuest() == null) {
                MessageUtil.sendMessage(player, "&7Quest: &6Keine");
                return;
            }
            MessageUtil.sendMessage(player, "&7Quest: &6" + region.getLinkedQuest().getName());
            return;
        }
        if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("c")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe eine ID an");
                return;
            }
            if (manager.getByID(args[2]) != null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Region existiert bereits.");
                return;
            }
            QRegion region = new QRegion(args[2]);
            manager.getRegions().add(region);
            MessageUtil.sendMessage(player, "<green>Region " + args[2] + " erfolgreich erstellt.");
            MessageUtil.sendMessage(player, "<gray>Setze nun die beiden Ecken der Region mit /q rg pos1 bzw. /q rg pos2.");
            return;
        }
        if (args[1].equalsIgnoreCase("pos1") || args[1].equalsIgnoreCase("p1")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe eine ID an");
                return;
            }
            QRegion region = manager.getByID(args[2]);
            if (region == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Region existiert nicht.");
                return;
            }
            region.setPos1(location);
            MessageUtil.sendMessage(player, "<green>Position 1 wurde erfolgreich gesetzt.");
            return;
        }
        if (args[1].equalsIgnoreCase("pos2") || args[1].equalsIgnoreCase("p2")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe eine ID an");
                return;
            }
            QRegion region = manager.getByID(args[2]);
            if (region == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Region existiert nicht.");
                return;
            }
            region.setPos2(location);
            MessageUtil.sendMessage(player, "<green>Position 2 wurde erfolgreich gesetzt.");
            return;
        }
        if (args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase("ls")) {
            for (QRegion region : manager.getRegions()) {
                MessageUtil.sendMessage(player, "&8- " + region.getId() + "&8: " + region.getNiceLocation());
            }
            return;
        }
        MessageUtil.sendMessage(player, QuestsXL.ERROR + "Unbekannter Befehl. Probiere z.B. /q rg i");
    }
}