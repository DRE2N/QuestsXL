package de.erethon.questsxl.scoreboard;

import de.erethon.aergia.player.EPlayer;
import de.erethon.aergia.scoreboard.ScoreboardLines;
import de.erethon.aergia.util.ScoreboardComponent;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.ActiveQuest;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fyreum
 */
public class QuestScoreboardLines implements ScoreboardLines {

    final QuestsXL plugin = QuestsXL.getInstance();

    @NotNull
    @Override
    public List<ScoreboardComponent> getLines(@NotNull EPlayer ePlayer) {
        return List.of();
        /*QPlayer player = plugin.getPlayerCache().getByPlayer(ePlayer.getPlayer());
        ActiveQuest trackedQuest = player.getTrackedQuest();

        if (trackedQuest == null && player.getTrackedEvent() == null) {
            return List.of();
        }
        List<ScoreboardComponent> lines = new ArrayList<>();

        if (player.getTrackedEvent() != null) {
            Component header = MessageUtil.parse("<green>" + player.getTrackedEvent().getName() + "</green>");

            lines.add(ScoreboardComponent.of((p) -> header));

            if (player.getTrackedEvent().getObjectiveDisplayText() != null) {
                for (String line : player.getTrackedEvent().getObjectiveDisplayText().split("<br>", 3)) {
                    lines.add(ScoreboardComponent.of((p) -> MessageUtil.parse("<gray>" + line + "<gray>")));
                }
            }
            lines.add(ScoreboardComponent.EMPTY);
        }

        if (trackedQuest != null) {
            Component header = MessageUtil.parse("<green>" + trackedQuest.getQuest().getDisplayName() + "</green>");

            lines.add(ScoreboardComponent.of((p) -> header));

            if (trackedQuest.getObjectiveDisplayText() != null) {
                for (String line : trackedQuest.getObjectiveDisplayText().split("<br>", 3)) {
                    lines.add(ScoreboardComponent.of((p) -> MessageUtil.parse("<gray>" + line + "<gray>")));
                }
            }
        }

        return lines;*/
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
