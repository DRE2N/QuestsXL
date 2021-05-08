package de.erethon.questsxl.players;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.objectives.ActiveObjective;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.quest.QStage;
import de.erethon.questsxl.tools.packetwrapper.WrapperPlayServerChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class QPlayer {

    Player player;
    BossBar bar;

    private final Map<ActiveQuest, Long> activeQuests = new HashMap<>();
    private final Map<QQuest, Long> startedQuests = new HashMap<>();
    private final Map<QQuest, Long> completedQuests = new HashMap<>();
    private final Set<ActiveObjective> currentObjectives = new HashSet<>();
    private final List<WrappedChatComponent> chatQueue = new CopyOnWriteArrayList<>();

    private QQuest editingQuest;
    private QStage editingStage;

    private boolean isInConversation = false;

    public QPlayer(Player player) {
        this.player = player;
        bar = Bukkit.getServer().createBossBar("qxl_" + player.getName(), BarColor.GREEN, BarStyle.SOLID);
        bar.addPlayer(player);
    }

    public BossBar getBar() {
        return bar;
    }

    public void startQuest(QQuest quest) {
        addActive(quest);
        startedQuests.put(quest, System.currentTimeMillis());
        MessageUtil.log("Active: " + activeQuests.keySet().size());
    }

    public void progressQuest(QQuest quest) {
        MessageUtil.log("Looking to progress " + quest.getName());
        MessageUtil.log("Quests: " + activeQuests.keySet().size());
        for (ActiveQuest active : activeQuests.keySet()) {
            if (active.getQuest() == quest) {
                active.progress(this);
            }
        }
    }

    public void addObjective(ActiveObjective objective) {
        currentObjectives.add(objective);
        MessageUtil.log(player.getName() + " now has " + currentObjectives.size() + " objectives.");
    }

    public void send(String msg) {
        MessageUtil.sendMessage(getPlayer(), msg);
    }

    public void addActive(QQuest quest) {
        ActiveQuest active = new ActiveQuest(this, quest);
        activeQuests.put(active, System.currentTimeMillis());
    }

    public void clearObjectives() {
        currentObjectives.clear();
    }

    public void removeObjective(ActiveObjective objective) {
        currentObjectives.remove(objective);
    }

    public void removeActive(ActiveQuest quest) {
        activeQuests.remove(quest);
    }

    public Map<QQuest, Long> getStartedQuests() {
        return startedQuests;
    }

    public Map<ActiveQuest, Long> getActiveQuests() {
        return activeQuests;
    }

    public Map<QQuest, Long> getCompletedQuests() {
        return completedQuests;
    }

    public Set<ActiveObjective> getCurrentObjectives() {
        return currentObjectives;
    }

    public QQuest getEditingQuest() {
        return editingQuest;
    }

    public void setEditingQuest(QQuest editingQuest) {
        this.editingQuest = editingQuest;
    }

    public QStage getEditingStage() {
        return editingStage;
    }

    public void setEditingStage(QStage editingStage) {
        this.editingStage = editingStage;
    }

    public boolean isInConversation() {
        return isInConversation;
    }

    public void setInConversation(boolean inConversation) {
        isInConversation = inConversation;
    }

    public void addChat(WrappedChatComponent chatComponent) {
        chatQueue.add(chatComponent);
    }

    public void sendMessagesInQueue() {
        if (chatQueue.isEmpty()) {
            return;
        }
        player.sendMessage(MiniMessage.get().parse("<hover:show_text:'<yellow><italic>Diese Nachrichten hast du verpasst,\nwährend du die Quest-Konversation gelesen hast.'><dark_gray>[...]"));
        for (WrappedChatComponent chatComponent : chatQueue) {
            WrapperPlayServerChat chat = new WrapperPlayServerChat();
            chat.setMessage(chatComponent);
            chat.setChatType(EnumWrappers.ChatType.CHAT);
            chat.sendPacket(player);
        }
        chatQueue.clear();
    }

    public void sendConversationMsg(String raw) {
        isInConversation = false;
        Component message = MiniMessage.get().parse(raw);
        player.sendMessage(message);
        isInConversation = true;
    }

    public boolean hasQuest(QQuest quest) {
        for (ActiveQuest q : activeQuests.keySet()) {
            if (q.getQuest() == quest) {
                return true;
            }
        }
        return false;
    }

    public List<WrappedChatComponent> getChatQueue() {
        return chatQueue;
    }

    public Player getPlayer() {
        return player;
    }
}
