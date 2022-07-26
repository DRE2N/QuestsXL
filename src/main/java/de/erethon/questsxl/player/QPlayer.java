package de.erethon.questsxl.player;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.config.storage.Nullability;
import de.erethon.bedrock.config.storage.StorageData;
import de.erethon.bedrock.config.storage.StorageDataContainer;
import de.erethon.bedrock.user.LoadableUser;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.dialogue.ActiveDialogue;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.ObjectiveHolder;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.Completable;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.region.QRegion;
import de.erethon.questsxl.tool.packetwrapper.WrapperPlayServerChat;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class QPlayer extends StorageDataContainer implements LoadableUser, ObjectiveHolder {

    public static final int CONFIG_VERSION = 1;

    UUID uuid;
    Player player;

    private final Map<ActiveQuest, Long> activeQuests = new HashMap<>();
    private final Map<QQuest, Long> completedQuests = new HashMap<>();
    private final Set<ActiveObjective> currentObjectives = new HashSet<>();
    private final List<WrappedChatComponent> chatQueue = new CopyOnWriteArrayList<>();

    private final Set<QRegion> currentRegions = new HashSet<>();
    @StorageData(type = HashMap.class, keyTypes = String.class, valueTypes = Integer.class, nullability = Nullability.IGNORE)
    private final Map<String, Integer> scores = new HashMap<>();

    private ActiveDialogue activeDialogue = null;
    private ActiveQuest displayed = null;
    private Location compassTarget = null;
    private boolean isInConversation = false;
    private boolean frozen = false;

    public QPlayer(@NotNull Player player) {
        super(QuestsXL.getPlayerFile(player.getUniqueId()), CONFIG_VERSION);
        this.uuid = player.getUniqueId();
        this.player = player;
        for (QObjective objective : QuestsXL.getInstance().getGlobalObjectives().getObjectives()) {
            currentObjectives.add(new ActiveObjective(this, null, objective));
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
        super.load(); // Important to still call this! Else the container won't work properly.
    }

    @Override
    public void saveData() {
        activeQuests.forEach((quest, started) -> config.set("activeQuests." + quest.getQuest().getName(), Map.of("currentStage", quest.getCurrentStage(), "started", started)));
        super.saveData(); // Important to still call this! Else the container won't work properly.
    }

    /* getter and setter */

    public void addScore(@NotNull String score, int amount) {
        setScore(score, scores.getOrDefault(score, 0) + amount);
    }

    public void removeScore(@NotNull String score, int amount) {
        setScore(score, scores.getOrDefault(score, 0) - amount);
    }

    public void setScore(@NotNull String score, int amount) {
        scores.put(score, amount);
    }

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

    public void addChat(@NotNull WrappedChatComponent chatComponent) {
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

    public void sendMessagesInQueue() {
        if (chatQueue.isEmpty()) {
            return;
        }
        player.sendMessage(MessageUtil.parse("<hover:show_text:'<yellow><italic>Diese Nachrichten hast du verpasst,\nwährend du die Quest-Konversation gelesen hast.'><dark_gray>[...]"));
        for (WrappedChatComponent chatComponent : chatQueue) {
            WrapperPlayServerChat chat = new WrapperPlayServerChat();
            chat.setMessage(chatComponent);
            chat.setChatType(EnumWrappers.ChatType.CHAT);
            chat.sendPacket(player);
        }
        chatQueue.clear();
    }

    public void sendConversationMsg(@NotNull String raw) {
        sendConversationMsg(MessageUtil.parse(raw));
    }

    public void sendConversationMsg(@NotNull Component message) {
        isInConversation = false;
        player.sendMessage(message);
        isInConversation = true;
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

    public @NotNull List<WrappedChatComponent> getChatQueue() {
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
}
