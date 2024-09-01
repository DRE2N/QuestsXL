package de.erethon.questsxl.objective;

import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLocation;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class ConsumeItemObjective extends QBaseObjective {

    private final HItemLibrary itemLibrary = QuestsXL.getInstance().getItemLibrary();

    private NamespacedKey itemID;

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof PlayerItemConsumeEvent e)) return;
        HItem item = itemLibrary.get(e.getItem()).getItem();
        if (item == null) return;
        if (item.getKey().equals(itemID)) {
            checkCompletion(active, this);
        }
    }

    @Override
    public void load(QLineConfig section) {
        itemID = NamespacedKey.fromString(section.getString("item"));
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        itemID = NamespacedKey.fromString(section.getString("item"));

    }
}
