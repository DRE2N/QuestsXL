package de.erethon.questsxl.scoreboard;

import de.erethon.aergia.player.EPlayer;
import de.erethon.aergia.scoreboard.ScoreboardLines;
import de.erethon.aergia.util.DynamicString;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.ActiveQuest;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Fyreum
 * pls fix
public class QuestScoreboardLines implements ScoreboardLines {

    final QuestsXL plugin = QuestsXL.getInstance();

    @Override
    public @NotNull List<DynamicString> getLines(@NotNull EPlayer ePlayer) {
        QPlayer qPlayer = plugin.getPlayerCache().getByUniqueId(ePlayer.getUniqueId());
        if (qPlayer == null) {
            return Collections.emptyList();
        }
        List<DynamicString> lines = new ArrayList<>();
        if (qPlayer.getTrackedQuest() != null) {
            lines.add(e -> "§aQuest: " + qPlayer.getTrackedQuest().getQuest().getDisplayName());
            lines.add(e -> "§a§o" + qPlayer.getTrackedQuest().getObjectiveDisplayText());
        }
        if (qPlayer.getTrackedEvent() != null) {
            lines.add(e -> " ");
            lines.add(e -> "§6Event: " + qPlayer.getTrackedEvent().getName());
            lines.add(e -> "§6§o" + qPlayer.getTrackedEvent().getObjectiveDisplayText());
        }
        return lines;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}*/
