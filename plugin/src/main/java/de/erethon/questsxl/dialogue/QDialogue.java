package de.erethon.questsxl.dialogue;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Fyreum
 */
public class QDialogue implements QComponent {

    File file;
    YamlConfiguration cfg;

    private final String name;
    private String senderName;
    private String npcId;
    private HashMap<Integer, QDialogueStage> stages;

    public QDialogue(File file) {
        this.name = file.getName().replace(".yml", "");
        this.file = file;
        this.cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getKeys(false).size() == 0) {
            QuestsXL.getInstance().getErrors().add(new FriendlyError("Dialog: " + name, "Datei ungültig.", "Datei " + file.getName() + " ist ungültig.", "Wahrscheinlich falsche Einrückung."));
            return;
        }
        this.stages = new HashMap<>();
        load();
    }

    public void load() {
        senderName = cfg.getString("sender", "Unbekannter");
        npcId = cfg.getString("npcId");
        ConfigurationSection stagesSection = cfg.getConfigurationSection("stages");
        String id = "Dialog: " + getName();
        if (stagesSection == null) {
            QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Stages konnten nicht geladen werden.", "stages section is null", "Wahrscheinlich falsche Einrückung."));
            return;
        }
        for (String key : stagesSection.getKeys(false)) {
            ConfigurationSection stageSection = stagesSection.getConfigurationSection(key);
            if (stageSection == null) {
                QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Stage '" + key + "' konnte nicht geladen werden", "stage section is null", "Wahrscheinlich falsche Einrückung."));
                continue;
            }
            try {
                int index = Integer.parseInt(key);
                QDialogueStage stage = new QDialogueStage(this, stageSection, index);
                stage.setParent(this);
                stages.put(index, stage);
            } catch (Exception e) {
                QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Stage '" + key + "' konnte nicht geladen werden", e.getMessage(), "Wahrscheinlich falsche Einrückung."));
            }
        }
    }

    public boolean canStart(QPlayer player) {
        return stages.get(0) != null && stages.get(0).canStart(player);
    }

    public ActiveDialogue start(Quester quester) {
        if (quester instanceof QPlayer player) {
            ActiveDialogue activeDialogue = new ActiveDialogue(player, this);
            activeDialogue.start();
            return activeDialogue;
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getNPCId() {
        return npcId;
    }

    public HashMap<Integer, QDialogueStage> getStages() {
        return stages;
    }

    public QDialogueStage getStageAtIndex(int index) {
        return stages.get(index);
    }

    @Override
    public QComponent getParent() {
        return null;
    }

    @Override
    public void setParent(QComponent parent) {
        // We are the top-level component, so we don't need to set a parent.
    }
}
