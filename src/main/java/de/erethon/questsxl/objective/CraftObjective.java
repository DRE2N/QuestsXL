package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QLineConfig;
import de.fyreum.jobsxl.user.event.UserCraftItemEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.CraftItemEvent;

public class CraftObjective extends QBaseObjective {

    String id;

    @Override
    public void check(ActiveObjective active, Event e) {
        if (e instanceof CraftItemEvent event) { // default crafting
            Player player = (Player) event.getWhoClicked();
            if (conditions(player) && event.getRecipe().getResult().getType().name().equalsIgnoreCase(id)) {
                checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(player));
            }
        } else if (e instanceof UserCraftItemEvent event) { // job crafting
            Player player = event.getUser().getPlayer();
            if (conditions(player) && event.getResult().getId().equalsIgnoreCase(id)) {
                checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(player));
            }
        }
    }

    @Override
    public void load(QLineConfig section) {
        super.load(section);
        id = section.getString("id");
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        id = section.getString("id");
    }
}
