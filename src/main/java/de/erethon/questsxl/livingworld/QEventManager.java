package de.erethon.questsxl.livingworld;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.objectives.ObjectiveManager;
import de.erethon.questsxl.objectives.QObjective;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class QEventManager {

    Set<QEvent> events = new HashSet<>();

    public QEvent getByID(String id) {
        return events.stream().filter(event -> id.equals(event.getId())).findFirst().orElse(null);
    }

    public void load(File file)  {
        for (File file1 : file.listFiles()) {
            events.add(new QEvent(file1));
        }
        for (QEvent event : events) {
            event.load();
        }
        MessageUtil.log("Loaded " + events.size() + " global objectives.");

    }

}
