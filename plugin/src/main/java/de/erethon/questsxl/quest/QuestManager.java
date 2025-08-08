package de.erethon.questsxl.quest;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuestManager {

    QuestsXL plugin = QuestsXL.get();

    Set<QQuest> quests = new HashSet<>();

    public QQuest getByName(String name) {
        QuestsXL.log("Searching for quest " + name + " Quests: " + quests.size());
        for (QQuest quest : quests) {
            if (quest.getName().equalsIgnoreCase(name)) {
                QuestsXL.log("Found " + quest.getName());
                return quest;
            }
        }
        return null;
    }

    public void load() {
        quests.clear();
        QuestsXL.log("Loading quests (" + QuestsXL.QUESTS.listFiles().length + ") files in quests folder)");
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
        QuestsXL.log("Loaded " + quests.size() + " quests.");
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

    public List<String> getQuestNames() {
        List<String> names = new ArrayList<>();
        for (QQuest quest : quests) {
            names.add(quest.getName());
        }
        return names;
    }

}
