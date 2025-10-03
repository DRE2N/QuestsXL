package de.erethon.questsxl.dialogue;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Fyreum
 */
public class QDialogue implements QComponent {

    File file;
    YamlConfiguration cfg;

    private final String name;
    private QTranslatable senderName;
    private String npcId;
    private HashMap<Integer, QDialogueStage> stages;
    private boolean canStartFromNPC = true;

    public QDialogue(File file) {
        this.name = file.getName().replace(".yml", "");
        this.file = file;
        this.cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getKeys(false).size() == 0) {
            QuestsXL.get().getErrors().add(new FriendlyError("Dialog: " + name, "Datei ungültig.", "Datei " + file.getName() + " ist ungültig.", "Wahrscheinlich falsche Einrückung."));
            return;
        }
        this.stages = new HashMap<>();
        load();
    }

    public void load() {
        if (cfg.isString("sender")) {
            String senderString = cfg.getString("sender", "Unbekannter");
            senderName = QTranslatable.fromString(senderString);
        } else if (cfg.isConfigurationSection("sender")) {
            ConfigurationSection senderSection = cfg.getConfigurationSection("sender");
            Map<Locale, String> translations = new HashMap<>();
            for (String key : senderSection.getKeys(false)) {
                if (key.matches("[a-zA-Z]{2,3}(-[a-zA-Z]{2,3})?")) {
                    Locale locale = Locale.forLanguageTag(key);
                    translations.put(locale, senderSection.getString(key));
                }
            }
            if (!translations.isEmpty()) {
                String syntheticKey = "qxl.dialogue.sender." + name;
                senderName = new QTranslatable(syntheticKey, translations);
            } else {
                senderName = QTranslatable.fromString("Unbekannter");
            }
        } else {
            senderName = QTranslatable.fromString("Unbekannter");
        }

        npcId = cfg.getString("npcId");
        canStartFromNPC = cfg.getBoolean("canStartFromNPC", true);
        ConfigurationSection stagesSection = cfg.getConfigurationSection("stages");
        String id = "Dialog: " + getName();
        if (stagesSection == null) {
            QuestsXL.get().getErrors().add(new FriendlyError(id, "Stages konnten nicht geladen werden.", "stages section is null", "Wahrscheinlich falsche Einrückung."));
            return;
        }
        for (String key : stagesSection.getKeys(false)) {
            ConfigurationSection stageSection = stagesSection.getConfigurationSection(key);
            if (stageSection == null) {
                QuestsXL.get().getErrors().add(new FriendlyError(id, "Stage '" + key + "' konnte nicht geladen werden", "stage section is null", "Wahrscheinlich falsche Einrückung."));
                continue;
            }
            try {
                int index = Integer.parseInt(key);
                QDialogueStage stage = new QDialogueStage(this, stageSection, index);
                stage.setParent(this);
                stages.put(index, stage);
            } catch (Exception e) {
                QuestsXL.get().getErrors().add(new FriendlyError(id, "Stage '" + key + "' konnte nicht geladen werden", e.getMessage(), "Wahrscheinlich falsche Einrückung."));
            }
        }

        // After all stages are loaded, validate and link dialogue option stage references
        for (QDialogueStage stage : stages.values()) {
            try {
                stage.validateAndLinkOptions();
            } catch (Exception e) {
                QuestsXL.get().getErrors().add(new FriendlyError(id, "Failed to validate dialogue options", e.getMessage(), "Invalid stage reference in dialogue options"));
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

    public QTranslatable getSenderName() {
        return senderName;
    }

    public String getNPCId() {
        return npcId;
    }

    public boolean canStartFromNPC() {
        return canStartFromNPC;
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
