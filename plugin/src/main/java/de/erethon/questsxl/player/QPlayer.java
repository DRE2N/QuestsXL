package de.erethon.questsxl.player;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.Completable;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.Scorable;
import de.erethon.questsxl.dialogue.ActiveDialogue;
import de.erethon.questsxl.global.GlobalObjectives;
import de.erethon.questsxl.livingworld.PlayerExplorer;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.region.QRegion;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QPlayer implements ObjectiveHolder, Scorable, Quester {

    private final QuestsXL plugin = QuestsXL.get();

    UUID uuid;
    Player player;
    ServerPlayer serverPlayer;

    private final Map<ActiveQuest, Long> activeQuests = new HashMap<>();
    private final Map<QQuest, Long> completedQuests = new HashMap<>();
    private final Set<ActiveObjective> currentObjectives = new HashSet<>();
    private final List<Component> chatQueue = new CopyOnWriteArrayList<>();
    private final List<String> dialogueRecollection = new ArrayList<>();

    private final Set<QRegion> currentRegions = new HashSet<>();
    private final Map<String, Integer> scores = new HashMap<>();

    private ActiveDialogue activeDialogue = null;
    private ActiveQuest displayed = null;
    private Location compassTarget = null;
    private boolean isInConversation = false;
    private boolean frozen = false;
    private volatile boolean dataLoaded = false;

    Pattern pattern = Pattern.compile("\"color\"\\s*:\\s*\"([^\"]*)\"");
    MiniMessage miniMessage = MiniMessage.miniMessage();

    private ActiveQuest trackedQuest;
    private QEvent trackedEvent;
    private int currentTrackedQuestPriority = 0;
    private int currentTrackedEventPriority = 0;

    private PlayerExplorer explorer;
    private Component explorerContentGuide;

    public QPlayer(@NotNull Player player) {
        // Note: Do not, ever, send any messages here. This will cause a recursion loop on player join.
        this.uuid = player.getUniqueId();
        this.player = player;
        CraftPlayer craftPlayer = (CraftPlayer) player;
        serverPlayer = craftPlayer.getHandle();
        explorer = new PlayerExplorer(this);
        GlobalObjectives globalObjectives = QuestsXL.get().getGlobalObjectives();
        if (globalObjectives != null) {
            for (QObjective objective : globalObjectives.getObjectives()) {
                ActiveObjective activeObjective = new ActiveObjective(this, globalObjectives, globalObjectives.getStages().getFirst(), objective);
                currentObjectives.add(activeObjective);
                plugin.getObjectiveEventManager().register(activeObjective);
            }
        }
        loadFromDatabase();
    }

    public void startQuest(@NotNull QQuest quest) {
        if (hasQuest(quest)) {
            return;
        }
        addActive(quest);
        setTrackedQuest(quest, 99); // Default priority for newly started quests
        QuestsXL.log("Active: " + activeQuests.keySet().size());
        saveToDatabase(); // Save to database when quest starts
    }

    public void progress(@NotNull Completable completable) {
        if (completable instanceof QQuest) {
            progressQuest((QQuest) completable);
        }
        if (completable instanceof QEvent) {
            progressEvent((QEvent) completable);
        }
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public String getUniqueId() {
        return player.getUniqueId().toString();
    }

    public void progressQuest(@NotNull QQuest quest) {
        QuestsXL.log("Looking to progress " + quest.getName());
        QuestsXL.log("Quests: " + activeQuests.keySet().size());
        for (ActiveQuest active : activeQuests.keySet()) {
            if (active.getQuest() == quest) {
                active.progress(this);
            }
        }
    }

    public void progressEvent(@NotNull QEvent event) {
        //
    }

    private void loadFromDatabase() {
        var databaseManager = QuestsXL.get().getDatabaseManager();
        if (databaseManager != null) {
            // Load asynchronously to avoid blocking the main thread
            databaseManager.loadPlayerData(this).thenAccept(result -> {
                dataLoaded = true;
            }).exceptionally(ex -> {
                QuestsXL.log("Failed to load player data for " + player.getName() + ": " + ex.getMessage());
                dataLoaded = true; // Mark as loaded even on error to avoid blocking
                return null;
            });
        } else {
            dataLoaded = true;
        }
    }

    public void saveToDatabase() {
        var databaseManager = QuestsXL.get().getDatabaseManager();
        if (databaseManager != null) {
            databaseManager.savePlayerData(this);
        }
    }

    public @NotNull Map<String, Integer> getScores() {
        return scores;
    }

    public void setExplorer(PlayerExplorer explorer) {
        this.explorer = explorer;
    }

    /* getter and setter */

    @Override
    public void addScore(@NotNull String score, int amount) {
        setScore(score, scores.getOrDefault(score, 0) + amount);
    }

    @Override
    public void removeScore(@NotNull String score, int amount) {
        setScore(score, scores.getOrDefault(score, 0) - amount);
    }

    @Override
    public void setScore(@NotNull String score, int amount) {
        scores.put(score, amount);
        saveToDatabase(); // Save to database when score changes
    }

    @Override
    public int getScore(@NotNull String id) {
        return scores.getOrDefault(id, 0);
    }

    @Override
    public void addObjective(@NotNull ActiveObjective objective) {
        currentObjectives.add(objective);
        QuestsXL.log(player.getName() + " now has " + currentObjectives.size() + " objectives.");
    }

    public void send(@NotNull String msg) {
        MessageUtil.sendMessage(getPlayer(), msg);
    }

    public void addActive(@NotNull QQuest quest) {
        ActiveQuest active = new ActiveQuest(this, quest);
        activeQuests.put(active, System.currentTimeMillis());
    }

    @Override
    public void clearObjectives() {
        for (ActiveObjective objective : currentObjectives) {
            plugin.getObjectiveEventManager().unregister(objective);
            // Remove from database when objectives are cleared
            objective.removeFromDatabase();
        }
        currentObjectives.clear();
    }

    @Override
    public void removeObjective(@NotNull ActiveObjective objective) {
        plugin.getObjectiveEventManager().unregister(objective);
        currentObjectives.remove(objective);
        // Remove from database when objective is removed
        objective.removeFromDatabase();
    }

    public void removeActive(@NotNull ActiveQuest quest) {
        activeQuests.remove(quest);
        setTrackedQuest(null, 99);
        saveToDatabase(); // Save to database when quest is removed
    }

    public @NotNull Map<ActiveQuest, Long> getActiveQuests() {
        return activeQuests;
    }

    public ActiveQuest getActiveQuest(QQuest quest) {
        for (ActiveQuest activeQuest : activeQuests.keySet()) {
            if (activeQuest.getQuest() == quest) {
                return activeQuest;
            }
        }
        return null;
    }

    public @NotNull Map<QQuest, Long> getCompletedQuests() {
        return completedQuests;
    }

    public void completeQuest(@NotNull QQuest quest, long completedAt) {
        completedQuests.put(quest, completedAt);
        saveToDatabase();
    }

    @Override
    public Set<ActiveObjective> getCurrentObjectives() {
        return currentObjectives;
    }

    @Override
    public boolean hasObjective(QObjective objective) {
        return currentObjectives.stream().anyMatch(o -> o.getObjective() == objective);
    }

    public boolean isInConversation() {
        return isInConversation;
    }

    public void setInConversation(boolean inConversation) {
        isInConversation = inConversation;
    }

    public void addChat(@NotNull Component chatComponent) {
        chatQueue.add(chatComponent);
    }

    public ActiveDialogue getActiveDialogue() {
        return activeDialogue;
    }

    public void setActiveDialogue(ActiveDialogue activeDialogue) {
        this.activeDialogue = activeDialogue;
    }

    public ActiveQuest getDisplayed() {
        return displayed;
    }

    public void setDisplayed(ActiveQuest displayed) {
        this.displayed = displayed;
    }

    public Location getCompassTarget() {
        return compassTarget;
    }

    public void setCompassTarget(Location compassTarget) {
        this.compassTarget = compassTarget;
    }

    public boolean isInRegion(@NotNull QRegion region) {
        return currentRegions.contains(region);
    }

    public @NotNull Set<QRegion> getRegions() {
        return currentRegions;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public void sendMessagesInQueue(boolean darkmode) {
        if (chatQueue.isEmpty()) {
            return;
        }
        if (darkmode) {
            for (int i = 0; i < 12; i++) {
                sendMarkedMessage(Component.empty());
            }
        }
        for (Component chatComponent : chatQueue) {
            String original = PaperAdventure.asJsonString(chatComponent, Locale.ENGLISH).replaceAll("\"color\":\"blue\"", "\"color\":\"dark_gray\"");
            Matcher matcher = pattern.matcher(original);
            String output = matcher.replaceAll("\"color\": \"dark_gray\"");

            Component darkComponent = GsonComponentSerializer.gson().deserialize(output);
            sendMarkedMessage(darkmode ? darkComponent : chatComponent);
        }
    }

    public void endDialogueAndSendRecollection(String sender) {
        StringBuilder hoverText = new StringBuilder();
        for (String s : dialogueRecollection) {
            hoverText.append(s + "\n");
        }
        Component recollection = Component.text("Dialog mit " + sender + " beendet.", NamedTextColor.GRAY)
                .hoverEvent(HoverEvent.showText(Component.text(hoverText.toString())))
                .decorate(TextDecoration.ITALIC);
        player.sendMessage(recollection);
    }

    public void sendConversationMsg(@NotNull QTranslatable translatable, QTranslatable senderName, int id, int max, boolean hasMoreMessages) {
        Component message = translatable.get();
        Component renderedMessage = GlobalTranslator.render(message, getPlayer().locale());
        renderedMessage = renderedMessage.colorIfAbsent(TextColor.color(220, 220, 220)); // A nice light gray

        sendMessagesInQueue(true);
        String plainSenderName = PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(senderName.get(), getPlayer().locale()));
        sendMarkedMessage(Component.empty());
        sendMarkedMessage(Component.empty());
        sendMarkedMessage(Component.empty());
        Component skipButton = Component.empty();
        if (hasMoreMessages) {
            skipButton = miniMessage.deserialize(" <gray>[<yellow><click:run_command:'/q dialogue next'>>></click></yellow>]")
                    .append(Component.text("").clickEvent(ClickEvent.runCommand("qxl_marker")));
        }
        Component header = miniMessage.deserialize("<green>                      <b>" + plainSenderName + "<!b> <dark_gray> -  <gray>[" + id + "/" + max + "]");
        sendMarkedMessage(header.append(skipButton));
        sendMarkedMessage(Component.empty());
        sendMarkedMessage(renderedMessage);
        dialogueRecollection.add(plainSenderName + ": " + PlainTextComponentSerializer.plainText().serialize(renderedMessage));
        sendMarkedMessage(Component.empty());

    }

    public void sendMarkedMessage(Component component) {
        player.sendMessage(component.append(Component.text("").clickEvent(ClickEvent.runCommand("qxl_marker"))));
    }

    public boolean hasQuest(@NotNull QQuest quest) {
        for (ActiveQuest q : activeQuests.keySet()) {
            if (q.getQuest() == quest) {
                return true;
            }
        }
        return false;
    }

    public @Nullable ActiveQuest getActive(@NotNull String name) {
        for (ActiveQuest q : activeQuests.keySet()) {
            if (q.getQuest().getName().equals(name)) {
                return q;
            }
        }
        return null;
    }

    public ActiveQuest getTrackedQuest() {
        return trackedQuest;
    }

    public void setTrackedQuest(QQuest trackedQuest, int priority) {
        if (trackedQuest == null) {
            this.trackedQuest = null;
            currentTrackedQuestPriority = 0;
            return;
        }
        if (priority < currentTrackedQuestPriority) {
            return;
        }
        for (ActiveQuest activeQuest : activeQuests.keySet()) {
            if (activeQuest.getQuest() == trackedQuest) {
                this.trackedQuest = activeQuest;
                currentTrackedQuestPriority = priority;
                return;
            }
        }
    }

    public @NotNull List<Component> getChatQueue() {
        return chatQueue;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public void sendMessage(Component component) {
        player.sendMessage(component);
    }

    public QEvent getTrackedEvent() {
        return trackedEvent;
    }

    public PlayerExplorer getExplorer() {
        return explorer;
    }

    public void setTrackedEvent(QEvent trackedEvent, int priority) {
        if (priority < currentTrackedEventPriority && trackedEvent != null) {
            return;
        }
        this.trackedEvent = trackedEvent;
        currentTrackedEventPriority = priority;
    }

    public void setContentGuideText(Component component) {
        explorerContentGuide = component;
    }

    public Component getContentGuideText() {
        return explorerContentGuide;
    }

    public int getCurrentTrackedEventPriority() {
        return currentTrackedEventPriority;
    }

    public UUID getUUID() {
        return uuid;
    }

    public boolean isDataLoaded() {
        return dataLoaded;
    }

    public static QPlayer get(Player player) {
        return QuestsXL.get().getDatabaseManager().getCurrentPlayer(player);
    }
}
