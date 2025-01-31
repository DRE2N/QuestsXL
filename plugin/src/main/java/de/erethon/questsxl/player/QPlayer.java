package de.erethon.questsxl.player;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.config.storage.Nullability;
import de.erethon.bedrock.config.storage.StorageData;
import de.erethon.bedrock.config.storage.StorageDataContainer;
import de.erethon.bedrock.user.LoadableUser;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.Completable;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.Scorable;
import de.erethon.questsxl.dialogue.ActiveDialogue;
import de.erethon.questsxl.global.GlobalObjectives;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.region.QRegion;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.html.HTMLScriptElement;

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

public class QPlayer extends StorageDataContainer implements LoadableUser, ObjectiveHolder, Scorable {

    public static final int CONFIG_VERSION = 1;

    UUID uuid;
    Player player;
    ServerPlayer serverPlayer;

    private final Map<ActiveQuest, Long> activeQuests = new HashMap<>();
    private final Map<QQuest, Long> completedQuests = new HashMap<>();
    private final Set<ActiveObjective> currentObjectives = new HashSet<>();
    private final List<Component> chatQueue = new CopyOnWriteArrayList<>();
    private final List<String> dialogueRecollection = new ArrayList<>();

    private final Set<QRegion> currentRegions = new HashSet<>();
    @StorageData(type = HashMap.class, keyTypes = String.class, valueTypes = Integer.class, nullability = Nullability.IGNORE)
    private final Map<String, Integer> scores = new HashMap<>();

    private ActiveDialogue activeDialogue = null;
    private ActiveQuest displayed = null;
    private Location compassTarget = null;
    private boolean isInConversation = false;
    private boolean frozen = false;

    Pattern pattern = Pattern.compile("\"color\"\\s*:\\s*\"([^\"]*)\"");
    MiniMessage miniMessage = MiniMessage.miniMessage();

    private ActiveQuest trackedQuest;
    private QEvent trackedEvent;
    private int currentTrackedQuestPriority = 0;
    private int currentTrackedEventPriority = 0;

    public QPlayer(@NotNull Player player) {
        super(QuestsXL.getPlayerFile(player.getUniqueId()), CONFIG_VERSION);
        this.uuid = player.getUniqueId();
        this.player = player;
        CraftPlayer craftPlayer = (CraftPlayer) player;
        serverPlayer = craftPlayer.getHandle();
        GlobalObjectives globalObjectives = QuestsXL.getInstance().getGlobalObjectives();
        if (globalObjectives != null) {
            for (QObjective objective : globalObjectives.getObjectives()) {
                currentObjectives.add(new ActiveObjective(this, globalObjectives, globalObjectives.getStages().getFirst(), objective));
            }
        }
        defaultLoadProcess();
        MessageUtil.sendMessage(player, "&2[QXL] &7Loaded " + currentObjectives.size() + " objectives.");
    }

    public void startQuest(@NotNull QQuest quest) {
        if (hasQuest(quest)) {
            return;
        }
        addActive(quest);
        MessageUtil.log("Active: " + activeQuests.keySet().size());
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

    public void progressQuest(@NotNull QQuest quest) {
        MessageUtil.log("Looking to progress " + quest.getName());
        MessageUtil.log("Quests: " + activeQuests.keySet().size());
        for (ActiveQuest active : activeQuests.keySet()) {
            if (active.getQuest() == quest) {
                active.progress(this);
            }
        }
    }

    public void progressEvent(@NotNull QEvent event) {
        //
    }

    /* LoadableUser methods */

    @Override
    public void onJoin(PlayerJoinEvent event) {
        player = event.getPlayer();
    }

    @Override
    public void onQuit(PlayerQuitEvent event) {
        if (activeDialogue != null) {
            activeDialogue.cancel();
        }
        activeDialogue = null;
        player = null;
    }

    @Override
    public void saveUser() {
        saveData();
    }

    /* StorageDataContainer methods */

    @Override
    public void load() {
        ConfigurationSection questsSection = config.getConfigurationSection("activeQuests");
        if (questsSection != null) {
            for (String questName : questsSection.getKeys(false)) {
                ConfigurationSection section = questsSection.getConfigurationSection(questName);
                if (section == null) continue;
                QQuest quest = QuestsXL.getInstance().getQuestManager().getByName(questName);
                if (quest == null) continue;
                int currentStage = section.getInt("currentStage");
                long started = section.getInt("started");
                activeQuests.put(new ActiveQuest(this, quest, currentStage), started);
            }
        }
        loadProgress(config);
        super.load(); // Important to still call this! Else the container won't work properly.
    }


    @Override
    public void saveData() {
        activeQuests.forEach((quest, started) -> config.set("activeQuests." + quest.getQuest().getName(), Map.of("currentStage", quest.getCurrentStage(), "started", started)));
        saveProgress(config);
        super.saveData(); // Important to still call this! Else the container won't work properly.
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
    }

    @Override
    public int getScore(@NotNull String id) {
        return scores.getOrDefault(id, 0);
    }

    @Override
    public void addObjective(@NotNull ActiveObjective objective) {
        currentObjectives.add(objective);
        MessageUtil.log(player.getName() + " now has " + currentObjectives.size() + " objectives.");
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
        currentObjectives.clear();
    }

    @Override
    public void removeObjective(@NotNull ActiveObjective objective) {
        currentObjectives.remove(objective);
    }

    public void removeActive(@NotNull ActiveQuest quest) {
        activeQuests.remove(quest);
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

    public void sendConversationMsg(@NotNull String raw, String senderName, int id, int max) {
        sendConversationMsg(MessageUtil.parse(raw), senderName, id, max);
    }

    public void sendConversationMsg(@NotNull Component message, String senderName, int id, int max) {
        sendMessagesInQueue(true);
        sendMarkedMessage(Component.empty());
        sendMarkedMessage(Component.empty());
        sendMarkedMessage(Component.empty());
        Component header = miniMessage.deserialize("<green>                      <b>" + senderName + "<!b> <dark_gray> -  <gray>[" + id + "/" + max + "]");
        sendMarkedMessage(header);
        sendMarkedMessage(Component.empty());
        sendMarkedMessage(message);
        dialogueRecollection.add(senderName + ": " + PlainTextComponentSerializer.plainText().serialize(message));
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

    @Override
    public void updatePlayer(Player player) {
        if (uuid.equals(player.getUniqueId())) {
            this.player = player;
        }
    }

    public QEvent getTrackedEvent() {
        return trackedEvent;
    }

    public void setTrackedEvent(QEvent trackedEvent, int priority) {
        if (priority < currentTrackedEventPriority) {
            return;
        }
        this.trackedEvent = trackedEvent;
        currentTrackedEventPriority = priority;
    }
}
