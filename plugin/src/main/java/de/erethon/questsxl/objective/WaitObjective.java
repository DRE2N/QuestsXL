package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

@QLoadableDoc(
        value = "wait",
        description = "This objective is completed after a certain amount of time has passed, or after a player has been in a certain location for a certain amount of time.",
        shortExample = "wait: duration=100",
        longExample = {
                "wait:",
                "  duration: 10",
                "  x: 100",
                "  y: 100",
                "  z: 100",
                "  range: 5"
        }
)
public class WaitObjective extends QBaseObjective {

    @QParamDoc(name = "duration", description = "The time in seconds that the objective holder has to wait.", required = true)
    long duration;
    // optional
    @QParamDoc(name = "location", description = "The location that the objective holder must be in to complete the objective.")
    QLocation location;
    @QParamDoc(name = "range", description = "How close the objective holder has to get to the location", def = "1")
    int distance;
    int durationWaited = 0;

    @Override
    public void onStart(ObjectiveHolder holder) {
        if (location == null) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> complete(holder, this, holder), duration);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (holder.getLocation().distance(location.get(holder.getLocation())) <= distance) {
                    if (++durationWaited >= duration) {
                        complete(holder, this, holder);
                    }
                } else {
                    durationWaited = 0;
                }
            }, 20, 20);
        }
    }

    @Override
    public void check(ActiveObjective active, Event event) {

    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        duration = cfg.getLong("duration");
        if (duration >= 0) {
            throw new RuntimeException("The wait objective in " + cfg.getName() + " contains a negative duration.");
        }
        location = cfg.getQLocation("location");
        distance = cfg.getInt("range", 1);
        if (distance <= 0) {
            throw new RuntimeException("The wait objective in " + cfg.getName() + " contains a negative range.");
        }
    }

    @Override
    public Class<?> getEventType() {
        return null;
    }
}
