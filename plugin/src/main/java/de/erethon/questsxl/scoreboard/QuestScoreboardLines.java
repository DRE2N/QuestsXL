package de.erethon.questsxl.scoreboard;

import de.erethon.aergia.player.EPlayer;
import de.erethon.aergia.scoreboard.ScoreboardComponent;
import de.erethon.aergia.scoreboard.ScoreboardLines;
import de.erethon.aergia.util.DynamicComponent;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.EventState;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.ActiveQuest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
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
        QPlayer player = plugin.getDatabaseManager().getCurrentPlayerByUUID(ePlayer.getUniqueId());
        if (player == null) {
            QuestsXL.log("Player " + ePlayer.getDisplayName() + " not found in database, cannot show quest scoreboard lines.");
            return List.of();
        }
        // Check if tracked event is still active, untrack if not
        if (player.getTrackedEvent() != null && player.getTrackedEvent().getState() != EventState.ACTIVE) {
            player.setTrackedEvent(null, 0);
        }

        ActiveQuest trackedQuest = player.getTrackedQuest();

        // Check conditions after potentially untracking the event
        if (trackedQuest == null && player.getTrackedEvent() == null && player.getContentGuideText() == null) {
            return List.of();
        }
        List<ScoreboardComponent> lines = new ArrayList<>();

        if (player.getContentGuideText() != null) {
            lines.add(ScoreboardComponent.of(new DynamicComponent() { // Is there a better way to do this?
                @Override
                public @NotNull Component get(EPlayer ePlayer) {
                    Component translatedContentGuide = GlobalTranslator.render(player.getContentGuideText(), ePlayer.getPlayer().locale());
                    return MessageUtil.parse(CONTENT_GUIDE_COLOR).append(translatedContentGuide);
                }
            }));
            lines.add(ScoreboardComponent.EMPTY);
        }

        if (player.getTrackedEvent() != null) {
            Component eventNameComponent = Component.translatable(player.getTrackedEvent().getName());
            Component translatedEventName = GlobalTranslator.render(eventNameComponent, ePlayer.getPlayer().locale());
            Component header = MessageUtil.parse(EVENT_COLOR).append(translatedEventName);

            lines.add(ScoreboardComponent.of((p) -> header));

            String objectiveText = getEventObjectiveDisplayText(player, player.getTrackedEvent());
            if (objectiveText != null) {
                for (String line : objectiveText.split("<br>", 3)) {
                    lines.add(ScoreboardComponent.of((p) -> MessageUtil.parse("<gray>" + line + "<gray>")));
                }
            }
            lines.add(ScoreboardComponent.EMPTY);
        }

        if (trackedQuest != null) {
            Component questNameComponent = Component.translatable(trackedQuest.getQuest().getDisplayName());
            Component translatedQuestName = GlobalTranslator.render(questNameComponent, ePlayer.getPlayer().locale());
            Component header = MessageUtil.parse(QUEST_COLOR).append(translatedQuestName);

            lines.add(ScoreboardComponent.of((p) -> header));

            String objectiveText = getObjectiveDisplayText(player, trackedQuest);
            if (objectiveText != null) {
                for (String line : objectiveText.split("<br>", 3)) {
                    lines.add(ScoreboardComponent.of((p) -> MessageUtil.parse("<gray>" + line + "<gray>")));
                }
            }
        }

        return lines;
    }

    /**
     * Gets the objective display text for a quest. Returns either the manually set text
     * or generates translated text from all active objectives.
     */
    private String getObjectiveDisplayText(QPlayer player, ActiveQuest activeQuest) {
        if (activeQuest.getObjectiveDisplayText() != null) {
            return activeQuest.getObjectiveDisplayText();
        }

        List<ActiveObjective> questObjectives = new ArrayList<>();
        for (ActiveObjective objective : player.getCurrentObjectives()) {
            if (objective.getCompletable() == activeQuest.getQuest()) {
                questObjectives.add(objective);
            }
        }
        return generateObjectiveDisplayText(player, questObjectives);
    }

    private String getEventObjectiveDisplayText(QPlayer player, QEvent event) {
        if (event.getObjectiveDisplayText() != null) {
            return event.getObjectiveDisplayText();
        }
        return generateObjectiveDisplayText(player, event.getCurrentObjectives());
    }

    private String generateObjectiveDisplayText(QPlayer player, Iterable<ActiveObjective> objectives) {
        List<String> objectiveLines = new ArrayList<>();

        for (ActiveObjective activeObjective : objectives) {
            if (activeObjective.isCompleted()) {
                continue; // Skip completed objectives
            }

            if (activeObjective.getObjective().isPersistent()) {
                continue; // Skip persistent objectives
            }

            var displayText = activeObjective.getObjective().getDisplayText(player.getPlayer());
            Component translatedComponent = GlobalTranslator.render(displayText.get(), player.getPlayer().locale());
            String translatedText = PlainTextComponentSerializer.plainText().serialize(translatedComponent);
            String progressText = "";
            if (activeObjective.getObjective().getProgressGoal() > 1) {
                progressText = " (" + activeObjective.getProgress() + "/" + activeObjective.getObjective().getProgressGoal() + ")";
            }

            translatedText = truncateSmartlyWithProgress(translatedText, progressText, 35);
            objectiveLines.add(translatedText);
        }

        return objectiveLines.isEmpty() ? null : String.join("<br>", objectiveLines);
    }

    /**
     * Smartly truncates text to fit within the scoreboard line limit while preserving progress information.
     * Progress information (X/Y) will always be shown, even if the line becomes longer than maxLength.
     */
    private String truncateSmartlyWithProgress(String text, String progressText, int maxLength) {
        String fullText = text + progressText;
        if (fullText.length() <= maxLength) {
            return fullText;
        }

        if (!progressText.isEmpty()) {
            int availableLength = maxLength - progressText.length() - 3; //  3 chars for "..."

            if (availableLength < 10) {
                String truncated = text.length() > 10 ? text.substring(0, 7) + "..." : text;
                return truncated + progressText;
            }

            String truncated = text.substring(0, availableLength);

            int lastSpace = truncated.lastIndexOf(' ');
            if (lastSpace > availableLength * 0.6) {
                truncated = truncated.substring(0, lastSpace);
            }

            return truncated + "..." + progressText;
        }
        return truncateSmartly(fullText, maxLength);
    }

    /**
     * Smartly truncates text to fit within the scoreboard line limit.
     * Tries to break at word boundaries and adds ellipsis when needed.
     */
    private String truncateSmartly(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        String truncated = text.substring(0, maxLength - 3); // 3 chars for "..."

        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > maxLength * 0.6) {
            truncated = truncated.substring(0, lastSpace);
        }

        return truncated + "...";
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
