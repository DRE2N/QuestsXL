package de.erethon.questsxl.objectives;

import de.erethon.questsxl.action.QAction;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

import java.util.Set;

public interface QObjective extends Listener {

    void check(Event event);

    boolean isOptional();
    boolean isFailed();
    boolean isPersistent();
    Set<QAction> getSuccessActions();
    Set<QAction> getFailActions();

    String getDisplayText();

    void load(ConfigurationSection section);
    void load(String[] msg);


}
