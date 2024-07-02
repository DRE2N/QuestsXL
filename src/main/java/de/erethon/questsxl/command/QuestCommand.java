package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.ActiveQuest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestCommand extends ECommand {

    QuestsXL plugin = QuestsXL.getInstance();

    public QuestCommand() {
        setCommand("info");
        setAliases("q", "quest");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("qxl.user.quest");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        QPlayer qPlayer = plugin.getPlayerCache().getByPlayer(player);
        if (args.length > 1 && args[1].equalsIgnoreCase("track")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Bitte gebe eine Quest an.");
                return;
            }
            ActiveQuest quest = qPlayer.getActive(args[2]);
            if (quest == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Quest ist nicht aktiv oder existiert nicht.");
                return;
            }
            qPlayer.setDisplayed(quest);
            MessageUtil.sendMessage(player, "&7Die Quest &a" + quest.getQuest().getDisplayName() + " &7wird nun getrackt.");
            return;
        }
        ActiveQuest active = qPlayer.getDisplayed();
        if (active == null) {
            if (args.length < 2) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Du hast keine getrackte Quest. Bitte gebe einen Quest-Namen an.");
                return;
            }
            ActiveQuest quest = qPlayer.getActive(args[1]);
            if (quest == null) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "Diese Quest existiert nicht oder sie ist nicht aktiv.");
                return;
            }
            MessageUtil.sendMessage(player, "&8&m               &r &2" + quest.getQuest().getDisplayName() + " &8&m               &r");
            MessageUtil.sendMessage(player, "&7&a" + quest.getCurrentStage().getDescription());
            for (QObjective objective : quest.getCurrentStage().getGoals()) {
                if (objective.isPersistent() && objective.isOptional()) {
                    continue;
                }
                MessageUtil.sendMessage(player, "&8- &a" + objective.getDisplayText());
            }
            return;
        }
        MessageUtil.sendMessage(player, "&8&m               &r &2" + active.getQuest().getDisplayName() + " &8&m               &r");
        MessageUtil.sendMessage(player, "&7&a" + active.getCurrentStage().getDescription());
        for (QObjective objective : active.getCurrentStage().getGoals()) {
            if (objective.isPersistent() && objective.isOptional()) {
                continue;
            }
            MessageUtil.sendMessage(player, "&8- &a" + objective.getDisplayText());
        }
    }
}