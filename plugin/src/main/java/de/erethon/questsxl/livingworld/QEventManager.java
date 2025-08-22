package de.erethon.questsxl.livingworld;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QEventManager {

    Set<QEvent> events = new HashSet<>();

    public QEventManager() {
        new EventUpdater().runTaskTimer(QuestsXL.get(), 100, 100); // Update events every 5 seconds
    }

    public QEvent getByID(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return events.stream().filter(event -> id.equals(event.getId())).findFirst().orElse(null);
    }

    public void load(File file)  {
        events.clear();
        for (File file1 : file.listFiles()) {
            QEvent event = null;
            try {
                event = new QEvent(file1);
            }
            catch (Throwable e) {
                QuestsXL.log("Failed to load event from " + file1.getName());
                e.printStackTrace();
                FriendlyError error = new FriendlyError(file1.getName(), "Failed to load event "+ file1.getName(), e.getMessage(),"").addStacktrace(e.getStackTrace());
                QuestsXL.get().addRuntimeError(error);
                continue;
            }
            if (event.isValid()) {
                events.add(event);
            }
        }
        QuestsXL.log("Loaded " + events.size() + " events.");
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

    public void addEvent(QEvent event) {
        events.add(event);
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
