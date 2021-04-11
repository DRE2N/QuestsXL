package de.erethon.questsxl.players;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.objectives.ActiveObjective;
import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.quest.QStage;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class QPlayer {

    Player player;
    BossBar bar;

    private final Map<ActiveQuest, Long> activeQuests = new HashMap<>();
    private final Map<QQuest, Long> startedQuests = new HashMap<>();
    private final Map<QQuest, Long> completedQuests = new HashMap<>();
    private final Set<ActiveObjective> currentObjectives = new HashSet<>();

    private QQuest editingQuest;
    private QStage editingStage;

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

    public Player getPlayer() {
        return player;
    }
}
