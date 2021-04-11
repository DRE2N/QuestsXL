package de.erethon.questsxl.quest;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.objectives.LocationObjective;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class QuestManager {

    QuestsXL plugin = QuestsXL.getInstance();

    Set<QQuest> quests = new HashSet<>();

    public QQuest getByName(String name) {
        MessageUtil.log("Searching for quest " + name + " Quests: " + quests.size());
        for (QQuest quest : quests) {
            MessageUtil.log(quest.toString());
            if (quest.getName().equals(name)) {
                return quest;
            }
        }
        return null;
    }

    public void load() {
        quests.clear();
        MessageUtil.log("Loading quests (" + QuestsXL.QUESTS.listFiles().length + " files in quests folder)");
        for (File file : QuestsXL.QUESTS.listFiles()){
            if (file.getName().contains("disabled")) {
                continue;
            }
            if (file.isDirectory()) {
                loadSub(file);
                continue;
            }
            quests.add(new QQuest(file));
        }
        MessageUtil.log("Loaded " + quests.size() + " quests.");
    }

    public void loadSub(File file) {
        for (File f : file.listFiles()){
            if (f.getName().contains("disabled")) {
                continue;
            }
            if (f.isDirectory()) {
                loadSub(f);
            }
            quests.add(new QQuest(f));
        }
    }

    public Set<QQuest> getQuests() {
        return quests;
    }

}
