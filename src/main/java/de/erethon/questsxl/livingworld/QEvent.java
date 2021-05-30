package de.erethon.questsxl.livingworld;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.players.QPlayer;
import de.erethon.questsxl.quest.Completable;
import de.erethon.questsxl.quest.QStage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class QEvent implements Completable {

    YamlConfiguration cfg;

    String id;
    List<QStage> stages = new ArrayList<>();
    EventState state;

    public QEvent(File file) {
        String fileName = file.getName();
        id = fileName.replace(".yml", "");
        cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getKeys(false).size() == 0) {
            QuestsXL.getInstance().getErrors().add(new FriendlyError("Event: " + this.getName(), "Datei ungültig.", "Datei " + file.getName() + " ist ungültig.", "Wahrscheinlich falsche Einrückung."));
            return;
        }
        load();
    }

    public String getName() {
        return null;
    }

    public void reward(QPlayer player) {

    }

    public void reward(Set<QPlayer> players) {

    }

    public List<QStage> getStages() {
        return stages;
    }

    public String getId() {
        return id;
    }

    public EventState getState() {
        return state;
    }

    public void load() {
        ConfigurationSection stageSection = cfg.getConfigurationSection("stages");
        if (stageSection == null) {
            MessageUtil.log("Event " + id + " does not contain any stages!");
            return;
        }
        for (String key : stageSection.getKeys(false)) {
            ConfigurationSection stageS = stageSection.getConfigurationSection(key);
            int id = Integer.parseInt(key);
            QStage stage = new QStage(this, id);
            try {
                stage.load(stageS);
            } catch (Exception e) {
                QuestsXL.getInstance().getErrors().add(new FriendlyError("Event: " + this.getName(), "Stage " + id + " konnte nicht geladen werden.", e.getMessage(), "..."));
            }
            stages.add(stage);
        }
        MessageUtil.log("Loaded event " + id + " with " + stages.size() + " stages.");
    }
}
