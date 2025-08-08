package de.erethon.questsxl.scoreboard;

import de.erethon.aergia.player.EPlayer;
import de.erethon.aergia.scoreboard.ScoreboardComponent;
import de.erethon.aergia.scoreboard.ScoreboardLines;
import de.erethon.aergia.util.DynamicComponent;
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

    final QuestsXL plugin = QuestsXL.get();

    private static final String QUEST_COLOR = "<dark_gray>[<#3fda52>♥<dark_gray>]<#3fda52> ";
    private static final String EVENT_COLOR = "<dark_gray>[<#ec762c>\uD83D\uDDE1<dark_gray>]<#ec762c> ";
    private static final String CONTENT_GUIDE_COLOR = "<dark_gray>[<#edcc4b>❄<dark_gray>]<#edcc4b> ";

    @NotNull
    @Override
    public List<ScoreboardComponent> getLines(@NotNull EPlayer ePlayer) {
        QPlayer player = plugin.getPlayerCache().getByPlayer(ePlayer.getPlayer());
        ActiveQuest trackedQuest = player.getTrackedQuest();

        if (trackedQuest == null && player.getTrackedEvent() == null && player.getContentGuideText() == null) {
            return List.of();
        }
        List<ScoreboardComponent> lines = new ArrayList<>();

        if (player.getContentGuideText() != null) {
            lines.add(ScoreboardComponent.of(new DynamicComponent() { // Is there a better way to do this?
                @Override
                public @NotNull Component get(EPlayer ePlayer) {
                    return MessageUtil.parse(CONTENT_GUIDE_COLOR + player.getContentGuideText());
                }
            }));
            lines.add(ScoreboardComponent.EMPTY);
        }

        if (player.getTrackedEvent() != null) {
            Component header = MessageUtil.parse(EVENT_COLOR + player.getTrackedEvent().getName());

            lines.add(ScoreboardComponent.of((p) -> header));

            if (player.getTrackedEvent().getObjectiveDisplayText() != null) {
                for (String line : player.getTrackedEvent().getObjectiveDisplayText().split("<br>", 3)) {
                    lines.add(ScoreboardComponent.of((p) -> MessageUtil.parse("<gray>" + line + "<gray>")));
                }
            }
            lines.add(ScoreboardComponent.EMPTY);
        }

        if (trackedQuest != null) {
            Component header = MessageUtil.parse(QUEST_COLOR + trackedQuest.getQuest().getDisplayName());

            lines.add(ScoreboardComponent.of((p) -> header));

            if (trackedQuest.getObjectiveDisplayText() != null) {
                for (String line : trackedQuest.getObjectiveDisplayText().split("<br>", 3)) {
                    lines.add(ScoreboardComponent.of((p) -> MessageUtil.parse("<gray>" + line + "<gray>")));
                }
            }
        }

        return lines;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
