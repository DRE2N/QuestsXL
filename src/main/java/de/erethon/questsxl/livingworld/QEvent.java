package de.erethon.questsxl.livingworld;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.Completable;
import de.erethon.questsxl.quest.QStage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class QEvent implements Completable {

    File file;
    YamlConfiguration cfg;

    String id;
    List<QStage> stages = new ArrayList<>();
    EventState state;
    // Timers
    int timeCurrent;
    int timeTrigger;

    public QEvent(File file) {
        String fileName = file.getName();
        this.file = file;
        id = fileName.replace(".yml", "");
        cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getKeys(false).size() == 0) {
            QuestsXL.getInstance().getErrors().add(new FriendlyError("Event: " + this.getName(), "Datei ungültig.", "Datei " + file.getName() + " ist ungültig.", "Wahrscheinlich falsche Einrückung."));
            return;
        }
        load();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void reward(QPlayer player) {

    }

    @Override
    public void reward(Set<QPlayer> players) {

    }

    @Override
    public List<QStage> getStages() {
        return stages;
    }

    public String getId() {
        return id;
    }

    public void update() {

    }

    public EventState getState() {
        return state;
    }

    @Override
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

    public void save() throws IOException {
        cfg.set("state.timeCurrent", timeCurrent);
        cfg.set("state.timeTrigger", timeTrigger);
        cfg.save(file);
    }
}
