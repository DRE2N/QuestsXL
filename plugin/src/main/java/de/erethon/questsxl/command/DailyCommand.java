package de.erethon.questsxl.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.PeriodicQuestManager;
import de.erethon.questsxl.quest.QQuest;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class DailyCommand extends ECommand {

    QuestsXL plugin = QuestsXL.get();

    public DailyCommand() {
        setCommand("daily");
        setRegisterSeparately(true);
        setMinArgs(0);
        setMaxArgs(1);
        setPlayerCommand(true);
        setHelp("View daily quests");
        setPermission("qxl.user.daily");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        PeriodicQuestManager manager = plugin.getPeriodicQuestManager();

        if (manager == null) {
            player.sendMessage(Component.translatable("qxl.periodic.system.disabled"));
            return;
        }

        if (!manager.isDailyEnabled()) {
            player.sendMessage(Component.translatable("qxl.daily.disabled"));
            return;
        }

        List<QQuest> dailyQuests = manager.getActiveDailyQuests();
        if (dailyQuests.isEmpty()) {
            player.sendMessage(Component.translatable("qxl.daily.none"));
            return;
        }

        int completed = manager.getDailyProgress(qPlayer);
        int total = dailyQuests.size();
        boolean allCompleted = manager.hasCompletedAllDaily(qPlayer);

        player.sendMessage(Component.translatable("qxl.daily.header"));

        if (allCompleted) {
            player.sendMessage(Component.translatable("qxl.daily.progress.complete",
                Component.text(completed), Component.text(total)));
        } else {
            player.sendMessage(Component.translatable("qxl.daily.progress",
                Component.text(completed), Component.text(total)));
        }

        LocalDateTime nextReset = manager.getNextDailyReset();
        String timeUntilReset = formatDuration(Duration.between(LocalDateTime.now(), nextReset));
        player.sendMessage(Component.translatable("qxl.daily.resets", Component.text(timeUntilReset)));
        player.sendMessage(Component.empty());

        for (QQuest quest : dailyQuests) {
            boolean completed_quest = qPlayer.getCompletedQuests().containsKey(quest);
            String status = completed_quest ? "&a✓" : "&7○";
            String questName = quest.displayName() != null ? quest.displayName().getAsString() : quest.getName();
            MessageUtil.sendMessage(player, status + " &7" + questName);
            if (!completed_quest && quest.getDescription() != null) {
                MessageUtil.sendMessage(player, "  &8" + quest.getDescription());
            }
        }

        if (allCompleted) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.translatable("qxl.daily.bonus.claimed"));
        }
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}


