package de.erethon.questsxl.conversation;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.EventState;
import de.erethon.questsxl.objective.ObjectiveManager;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.quest.QStage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class QConversation {

    File file;
    YamlConfiguration cfg;
    QuestsXL plugin = QuestsXL.getInstance();

    String id;
    Map<String, Set<QObjective>> triggers = new HashMap<>();

    public QConversation(File file) {
        String fileName = file.getName();
        this.file = file;
        id = fileName.replace(".yml", "");
        cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getKeys(false).size() == 0) {
            plugin.getErrors().add(new FriendlyError("Conversation: " + this.getID(), "Datei ungültig.", "Datei " + file.getName() + " ist ungültig.", "Wahrscheinlich falsche Einrückung."));
            return;
        }
        load();
    }

    public String getID() {
        return id;
    }

    public void load() {
        ConfigurationSection stageSection = cfg.getConfigurationSection("triggers");
        if (stageSection == null) {
            plugin.getErrors().add(new FriendlyError("Conversation: " + this.getID(), "Keine Trigger angegeben für " + this.getID(), "", ""));
            return;
        }
        

    }

    public void save() throws IOException {

        cfg.save(file);
    }
}
