package de.erethon.questsxl.objective.event;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QObjective;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectiveEventManager {

    private final QuestsXL plugin;
    private final Map<Class<? extends Event>, CentralizedListener<?>> centralListeners = new ConcurrentHashMap<>();

    public ObjectiveEventManager(QuestsXL plugin) {
        this.plugin = plugin;
    }

    /**
     * Activates an objective, registering its required event listener.
     *
     * @param objective The active objective instance.
     */
    public void register(ActiveObjective objective) {
        QObjective<?> qObjective = objective.getObjective();
        Class<? extends Event> eventType = qObjective.getEventType();
        if (eventType == null) {
            return; // Impossible objective or similar case
        }

        CentralizedListener<?> central = centralListeners.computeIfAbsent(eventType, key -> {
            CentralizedListener newCentralListener = new CentralizedListener<>();
            Bukkit.getPluginManager().registerEvent(
                    key,
                    newCentralListener,
                    org.bukkit.event.EventPriority.NORMAL,
                    newCentralListener,
                    plugin,
                    false
            );
            return newCentralListener;
        });

        central.addHandler(objective);
    }

    /**
     * Deactivates an objective, unregistering its event listener.
     *
     * @param objective The active objective instance to remove.
     */
    public void unregister(ActiveObjective objective) {
        QObjective<?> qObjective = objective.getObjective();
        Class<? extends Event> eventType = qObjective.getEventType();
        CentralizedListener<?> central = centralListeners.get(eventType);

        if (central != null) {
            central.removeHandler(objective);
            // If that was the last objective for this event type, clean up the listener.
            if (central.isEmpty()) {
                HandlerList.unregisterAll(central);
                centralListeners.remove(eventType);
            }
        }
    }
    /**
     * Unregisters all objectives and their associated event listeners.
     */
    public void unregisterAll() {
        centralListeners.values().forEach(HandlerList::unregisterAll);
        centralListeners.clear();
    }

    /**
     * A private, generic, centralized listener that dispatches a specific Event type
     * to multiple QObjectives.
     */
    @SuppressWarnings("unchecked")
    private static class CentralizedListener<T extends Event> implements Listener, EventExecutor {
        private final Set<ActiveObjective> handlers = ConcurrentHashMap.newKeySet();

        @Override
        public void execute(@NotNull Listener listener, @NotNull Event event) {
            T specificEvent = (T) event;

            for (ActiveObjective activeObjective : handlers) {
                QObjective<T> qObjective = (QObjective<T>) activeObjective.getObjective();
                try {
                    qObjective.check(activeObjective, specificEvent);
                } catch (Exception e) {
                    System.err.println("Error executing objective check for " + event.getEventName());
                    e.printStackTrace();
                }
            }
        }

        void addHandler(ActiveObjective objective) {
            handlers.add(objective);
        }

        void removeHandler(ActiveObjective objective) {
            handlers.remove(objective);
        }

        boolean isEmpty() {
            return handlers.isEmpty();
        }
    }
}