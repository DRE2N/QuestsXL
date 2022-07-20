package de.erethon.questsxl.livingworld;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class QEventManager {

    Set<QEvent> events = new HashSet<>();

    public QEventManager() {
        new EventUpdater().runTaskTimer(QuestsXL.getInstance(), 100, 100); // Update events every 5 seconds
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

    public void save() {
        for (QEvent event : events){
            event.save();
        }
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
