package de.erethon.questsxl.scoreboard;

import de.erethon.aergia.player.EPlayer;
import de.erethon.aergia.scoreboard.ScoreboardLines;
import de.erethon.aergia.util.DynamicString;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Fyreum
 */
public class QuestScoreboardLines implements ScoreboardLines {

    public static final int MAXIMUM_QUEST_LINES = 4;

    final QuestsXL plugin = QuestsXL.getInstance();

    @Override
    public @NotNull List<DynamicString> getLines(@NotNull EPlayer ePlayer) {
        QPlayer qPlayer = plugin.getPlayerCache().getByUniqueId(ePlayer.getUniqueId());
        if (qPlayer == null) {
            return Collections.emptyList();
        }
        Set<ActiveObjective> objectives = qPlayer.getCurrentObjectives();
        List<DynamicString> lines = new ArrayList<>(Math.min(objectives.size(), MAXIMUM_QUEST_LINES + 1));

        for (ActiveObjective objective : objectives) {
            if (lines.size() >= MAXIMUM_QUEST_LINES) {
                lines.add(p -> ChatColor.translateAlternateColorCodes('&', "&e» &6" + (objectives.size() - MAXIMUM_QUEST_LINES) + " &7weitere"));
                break;
            }
            lines.add(p -> ChatColor.translateAlternateColorCodes('&', "&e» &7" + objective.getMessage()));
        }
        return lines;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
