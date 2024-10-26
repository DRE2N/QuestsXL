package de.erethon.questsxl.livingworld;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        events.clear();
        for (File file1 : file.listFiles()) {
            QEvent event = new QEvent(file1);
            if (event.isValid()) {
                events.add(event);
                event.loadProgress(event.getCfg());
            }
        }
        MessageUtil.log("Loaded " + events.size() + " events.");
    }

    public void save() {
        for (QEvent event : events){
            event.save();
        }
    }

    public Set<QEvent> getEvents() {
        return events;
    }

    public Set<QEvent> getActiveEvents() {
        return events.stream().filter(event -> event.getState() == EventState.ACTIVE).collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    public List<String> getEventIDs() {
        List<String> ids = new ArrayList<>();
        for (QEvent event : events) {
            ids.add(event.getId());
        }
        return ids;
    }

    public List<String> getActiveEventIDs() {
        List<String> ids = new ArrayList<>();
        for (QEvent event : events) {
            if (event.getState() == EventState.ACTIVE) {
                ids.add(event.getId());
            }
        }
        return ids;
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
