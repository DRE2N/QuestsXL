package de.erethon.questsxl.commands;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.players.QPlayer;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestCommand extends DRECommand {

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
        QPlayer qPlayer = plugin.getPlayerCache().get(player);
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
            MessageUtil.sendMessage(player, quest.getCurrentStage().getDescription());
            return;
        }
        MessageUtil.sendMessage(player, active.getCurrentStage().getDescription());
    }
}