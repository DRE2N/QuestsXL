package de.erethon.questsxl.player;

import de.erethon.questsxl.quest.ActiveQuest;
import de.erethon.questsxl.quest.QQuest;

import java.util.HashSet;
import java.util.Set;

public class QGroup {

    private QPlayer leader;
    private final Set<QPlayer> members = new HashSet<>();

    public QGroup(QPlayer leader) {
        this.leader = leader;
    }

    public void addMember(QPlayer player) {
        members.add(player);
    }

    public void removeMember(QPlayer player) {
        members.remove(player);
    }

    public void setLeader(QPlayer player) {
        leader = player;
    }

    public void tryAcceptQuest(QQuest quest) {
        for (QPlayer player : members) {
            if (!quest.canStartQuest(player)) {
                for (QPlayer player1 : members) {
                    player1.send("Group member " + player.getPlayer().getName() + " can't start quest. Try again to start the quest for all members that are able to start it.");
                }
                return;
            }
            player.startQuest(quest);
        }
    }

    public void forceAcceptQuest(QQuest quest) {
        for (QPlayer player : members) {
            if (!quest.canStartQuest(player)) {
                continue;
            }
            player.startQuest(quest);
        }
    }

    public void completeQuest(ActiveQuest quest) {
        for (QPlayer player : members) {
            if (player.getCompletedQuests().containsKey(quest.getQuest())) {
                quest.finishWithoutRewards(player);
            } else {
                quest.finish(player);
            }
        }
    }

    public QPlayer getLeader() {
        return leader;
    }

    public Set<QPlayer> getMembers() {
        return members;
    }
}
