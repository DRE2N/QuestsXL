package de.erethon.questsxl.scoreboard;

import de.erethon.aergia.player.EPlayer;
import de.erethon.aergia.scoreboard.ScoreboardLines;
import de.erethon.aergia.util.DynamicString;
import de.erethon.aergia.util.ScoreboardComponent;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QStage;
import de.erethon.questsxl.objective.QObjective;
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
        QPlayer player = plugin.getPlayerCache().getByPlayer(ePlayer.getPlayer());
        ActiveQuest trackedQuest = player.getTrackedQuest();

        if (trackedQuest == null) {
            return List.of();
        }
        QStage currentStage = trackedQuest.getCurrentStage();
        List<ScoreboardComponent> lines = new ArrayList<>();

        Component header = MessageUtil.parse("<green>" + trackedQuest.getQuest().getDisplayName() + "</green>");
        lines.add(ScoreboardComponent.of((unused) -> header, false));

        for (QObjective objective : currentStage.getGoals()) {
            Component description = MessageUtil.parse("<gray>" + objective.getDisplayText() + "</gray>");
            lines.add(ScoreboardComponent.of((unused) -> description, false));
        }

        return lines;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
