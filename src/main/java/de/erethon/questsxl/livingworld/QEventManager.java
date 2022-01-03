package de.erethon.questsxl.livingworld;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.objectives.ObjectiveManager;
import de.erethon.questsxl.objectives.QObjective;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class QEventManager {

    Set<QEvent> events = new HashSet<>();

    public QEventManager() {
        new EventUpdater().runTaskTimer(QuestsXL.getInstance(), 1200, 1200); // Update events every minute
    }

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
        MessageUtil.log("Loaded " + events.size() + " events.");

    }
    class EventUpdater extends BukkitRunnable {
        @Override
        public void run() {
            for (QEvent event : events) {
                event.update();
            }
        }
    }

}
