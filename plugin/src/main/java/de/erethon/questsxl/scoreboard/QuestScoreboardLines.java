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
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class QuestScoreboardLines implements ScoreboardLines {

    final QuestsXL plugin = QuestsXL.get();

    // Cache for max line length per player
    private final Map<UUID, CachedScoreboardData> playerCache = new HashMap<>();

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
            playerCache.remove(ePlayer.getUniqueId());
            return List.of();
        }

        int maxLineLength = getCachedMaxLineLength(player, ePlayer, trackedQuest);

        List<ScoreboardComponent> lines = new ArrayList<>();

        if (player.getContentGuideText() != null) {
            lines.add(ScoreboardComponent.of(new DynamicComponent() { // Is there a better way to do this?
                @Override
                public @NotNull Component get(EPlayer ePlayer) {
                    Component translatedContentGuide = GlobalTranslator.render(player.getContentGuideText(), ePlayer.getPlayer().locale());
                    translatedContentGuide = translatedContentGuide.color(TextColor.fromCSSHexString("#edcc4b"));
                    return MessageUtil.parse(CONTENT_GUIDE_COLOR).append(translatedContentGuide);
                }
            }));
            lines.add(ScoreboardComponent.EMPTY);
        }

        if (player.getTrackedEvent() != null) {
            Component translatedEventName = GlobalTranslator.render(player.getTrackedEvent().displayName().get(), ePlayer.getPlayer().locale());
            translatedEventName = translatedEventName.color(TextColor.fromCSSHexString("#ec762c"));
            Component header = MessageUtil.parse(EVENT_COLOR).append(translatedEventName);

            lines.add(ScoreboardComponent.of((p) -> header));

            List<Component> objectiveComponents = getEventObjectiveDisplayComponents(player, player.getTrackedEvent(), maxLineLength);
            for (Component component : objectiveComponents) {
                lines.add(ScoreboardComponent.of((p) -> component));
            }
            lines.add(ScoreboardComponent.EMPTY);
        }

        if (trackedQuest != null) {
            Component translatedQuestName = GlobalTranslator.render(trackedQuest.getQuest().displayName().get(), ePlayer.getPlayer().locale());
            translatedQuestName = translatedQuestName.color(TextColor.fromCSSHexString("#3fda52"));
            Component header = MessageUtil.parse(QUEST_COLOR).append(translatedQuestName);

            lines.add(ScoreboardComponent.of((p) -> header));

            List<Component> objectiveComponents = getObjectiveDisplayComponents(player, trackedQuest, maxLineLength);
            for (Component component : objectiveComponents) {
                lines.add(ScoreboardComponent.of((p) -> component));
            }
        }

        return lines;
    }

    /**
     * Gets the cached max line length or calculates it if the state has changed.
     */
    private int getCachedMaxLineLength(QPlayer player, EPlayer ePlayer, ActiveQuest trackedQuest) {
        UUID playerId = ePlayer.getUniqueId();
        ScoreboardStateKey currentState = new ScoreboardStateKey(player, trackedQuest);

        CachedScoreboardData cached = playerCache.get(playerId);

        if (cached != null && cached.stateKey.equals(currentState)) {
            return cached.maxLineLength;
        }

        int maxLineLength = calculateMaxLineLength(player, ePlayer, trackedQuest);

        // Update cache
        playerCache.put(playerId, new CachedScoreboardData(currentState, maxLineLength));

        return maxLineLength;
    }

    /**
     * Calculates the maximum line length among all scoreboard entries to determine
     * the optimal progress bar width.
     */
    private int calculateMaxLineLength(QPlayer player, EPlayer ePlayer, ActiveQuest trackedQuest) {
        int maxLength = 0;

        // Check content guide text
        if (player.getContentGuideText() != null) {
            Component translatedContentGuide = GlobalTranslator.render(player.getContentGuideText(), ePlayer.getPlayer().locale());
            Component fullLine = MessageUtil.parse(CONTENT_GUIDE_COLOR).append(translatedContentGuide);
            int length = PlainTextComponentSerializer.plainText().serialize(fullLine).length();
            maxLength = Math.max(maxLength, length);
        }

        // Check event name and objectives
        if (player.getTrackedEvent() != null) {
            Component translatedEventName = GlobalTranslator.render(player.getTrackedEvent().displayName().get(), ePlayer.getPlayer().locale());
            Component header = MessageUtil.parse(EVENT_COLOR).append(translatedEventName);
            int length = PlainTextComponentSerializer.plainText().serialize(header).length();
            maxLength = Math.max(maxLength, length);

            // Check event objectives
            int eventObjLength = calculateObjectivesMaxLength(player, player.getTrackedEvent().getCurrentObjectives(), ePlayer);
            maxLength = Math.max(maxLength, eventObjLength);
        }

        // Check quest name and objectives
        if (trackedQuest != null) {
            Component translatedQuestName = GlobalTranslator.render(trackedQuest.getQuest().displayName().get(), ePlayer.getPlayer().locale());
            Component header = MessageUtil.parse(QUEST_COLOR).append(translatedQuestName);
            int length = PlainTextComponentSerializer.plainText().serialize(header).length();
            maxLength = Math.max(maxLength, length);

            // Check quest objectives
            List<ActiveObjective> questObjectives = new ArrayList<>();
            for (ActiveObjective objective : player.getCurrentObjectives()) {
                if (objective.getCompletable() == trackedQuest.getQuest()) {
                    questObjectives.add(objective);
                }
            }
            int questObjLength = calculateObjectivesMaxLength(player, questObjectives, ePlayer);
            maxLength = Math.max(maxLength, questObjLength);
        }

        // Ensure a minimum length for the progress bar
        return Math.max(maxLength, 20);
    }

    /**
     * Calculates the maximum length among objective text entries.
     */
    private int calculateObjectivesMaxLength(QPlayer player, Iterable<ActiveObjective> objectives, EPlayer ePlayer) {
        int maxLength = 0;

        for (ActiveObjective activeObjective : objectives) {
            if (activeObjective.isCompleted() || activeObjective.getObjective().isPersistent() || activeObjective.getObjective().isHidden()) {
                continue;
            }

            var displayText = activeObjective.getObjective().getDisplayText(player.getPlayer());
            Component translatedComponent = GlobalTranslator.render(displayText.get(), player.getPlayer().locale());
            String translatedText = PlainTextComponentSerializer.plainText().serialize(translatedComponent);
            String progressText = "";
            if (activeObjective.getObjective().getProgressGoal() > 1) {
                progressText = " (" + activeObjective.getProgress() + "/" + activeObjective.getObjective().getProgressGoal() + ")";
            }

            String fullText = translatedText + progressText;
            maxLength = Math.max(maxLength, fullText.length());
        }

        return maxLength;
    }

    /**
     * Gets the objective display text for a quest. Returns either the manually set text
     * or generates translated text from all active objectives.
     */
    private List<Component> getObjectiveDisplayComponents(QPlayer player, ActiveQuest activeQuest, int maxLineLength) {
        if (activeQuest.getObjectiveDisplayText() != null) {
            return List.of(MessageUtil.parse("<gray>" + activeQuest.getObjectiveDisplayText() + "<gray>"));
        }

        List<ActiveObjective> questObjectives = new ArrayList<>();
        for (ActiveObjective objective : player.getCurrentObjectives()) {
            if (objective.getCompletable() == activeQuest.getQuest()) {
                questObjectives.add(objective);
            }
        }
        return generateObjectiveDisplayComponents(player, questObjectives, true, maxLineLength);
    }

    private List<Component> getEventObjectiveDisplayComponents(QPlayer player, QEvent event, int maxLineLength) {
        if (event.getObjectiveDisplayText() != null) {
            return List.of(MessageUtil.parse("<gray>" + event.getObjectiveDisplayText() + "<gray>"));
        }
        return generateObjectiveDisplayComponents(player, event.getCurrentObjectives(), false, maxLineLength);
    }

    private List<Component> generateObjectiveDisplayComponents(QPlayer player, Iterable<ActiveObjective> objectives, boolean isQuest, int maxLineLength) {
        List<Component> objectiveComponents = new ArrayList<>();

        for (ActiveObjective activeObjective : objectives) {
            if (activeObjective.isCompleted()) {
                continue; // Skip completed objectives
            }

            if (activeObjective.getObjective().isPersistent()) {
                continue; // Skip persistent objectives
            }
            if (activeObjective.getObjective().isHidden()) {
                continue; // Skip hidden objectives
            }

            var displayText = activeObjective.getObjective().getDisplayText(player.getPlayer());
            Component translatedComponent = GlobalTranslator.render(displayText.get(), player.getPlayer().locale());
            String translatedText = PlainTextComponentSerializer.plainText().serialize(translatedComponent);
            String progressText = "";
            if (activeObjective.getObjective().getProgressGoal() > 1) {
                progressText = " (" + activeObjective.getProgress() + "/" + activeObjective.getObjective().getProgressGoal() + ")";
            }

            translatedText = truncateSmartlyWithProgress(translatedText, progressText, 35);

            String displayLine = translatedText.trim();
            objectiveComponents.add(MessageUtil.parse("<gray>" + displayLine));

            if (activeObjective.getObjective().getProgressGoal() > 1) {
                Component progressBar = getObjectiveProgressBar(activeObjective, isQuest, maxLineLength);
                objectiveComponents.add(progressBar);
            }
        }

        return objectiveComponents;
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

    private Component getObjectiveProgressBar(ActiveObjective activeObjective, boolean isQuest, int maxLineLength) {
        int goal = activeObjective.getObjective().getProgressGoal();

        int progressBarWidth = Math.max(maxLineLength, 20);

        TextColor filledColor = isQuest ?
            TextColor.fromCSSHexString("#3fda52") :  // Quest green
            TextColor.fromCSSHexString("#ec762c");   // Event orange
        TextColor emptyColor = TextColor.fromCSSHexString("#404040"); // Dark gray for empty

        Component filledSegment = Component.text(" ").color(filledColor).decorate(TextDecoration.STRIKETHROUGH);
        Component emptySegment = Component.text(" ").color(emptyColor).decorate(TextDecoration.STRIKETHROUGH);

        int filledSegments = (int) Math.round(((double) activeObjective.getProgress() / goal) * progressBarWidth);

        Component progressBar = Component.empty();
        for (int i = 0; i < progressBarWidth; i++) {
            if (i < filledSegments) {
                progressBar = progressBar.append(filledSegment);
            } else {
                progressBar = progressBar.append(emptySegment);
            }
        }
        return progressBar;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    /**
     * Cache key to track the state of a player's scoreboard to detect changes.
     */
    private static class ScoreboardStateKey {
        private final String contentGuideText;
        private final String trackedQuestId;
        private final String trackedEventId;
        private final List<ObjectiveState> objectives;

        ScoreboardStateKey(QPlayer player, ActiveQuest trackedQuest) {
            this.contentGuideText = player.getContentGuideText() != null ?
                PlainTextComponentSerializer.plainText().serialize(player.getContentGuideText()) : null;
            this.trackedQuestId = trackedQuest != null ? trackedQuest.getQuest().id() : null;
            this.trackedEventId = player.getTrackedEvent() != null ? player.getTrackedEvent().id() : null;

            this.objectives = new ArrayList<>();

            // Collect quest objectives
            if (trackedQuest != null) {
                for (ActiveObjective obj : player.getCurrentObjectives()) {
                    if (obj.getCompletable() == trackedQuest.getQuest() &&
                        !obj.isCompleted() && !obj.getObjective().isPersistent() && !obj.getObjective().isHidden()) {
                        objectives.add(new ObjectiveState(obj));
                    }
                }
            }

            // Collect event objectives
            if (player.getTrackedEvent() != null) {
                for (ActiveObjective obj : player.getTrackedEvent().getCurrentObjectives()) {
                    if (!obj.isCompleted() && !obj.getObjective().isPersistent() && !obj.getObjective().isHidden()) {
                        objectives.add(new ObjectiveState(obj));
                    }
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScoreboardStateKey that = (ScoreboardStateKey) o;
            return Objects.equals(contentGuideText, that.contentGuideText) &&
                   Objects.equals(trackedQuestId, that.trackedQuestId) &&
                   Objects.equals(trackedEventId, that.trackedEventId) &&
                   Objects.equals(objectives, that.objectives);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contentGuideText, trackedQuestId, trackedEventId, objectives);
        }
    }

    /**
     * Represents the state of an objective for cache comparison.
     */
    private static class ObjectiveState {
        private final String objectiveId;
        private final int progress;
        private final int goal;

        ObjectiveState(ActiveObjective objective) {
            this.objectiveId = objective.getObjective().id();
            this.progress = objective.getProgress();
            this.goal = objective.getObjective().getProgressGoal();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ObjectiveState that = (ObjectiveState) o;
            return progress == that.progress &&
                   goal == that.goal &&
                   Objects.equals(objectiveId, that.objectiveId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(objectiveId, progress, goal);
        }
    }

    /**
     * Cached scoreboard data for a player.
     */
    private static class CachedScoreboardData {
        private final ScoreboardStateKey stateKey;
        private final int maxLineLength;

        CachedScoreboardData(ScoreboardStateKey stateKey, int maxLineLength) {
            this.stateKey = stateKey;
            this.maxLineLength = maxLineLength;
        }
    }
}
