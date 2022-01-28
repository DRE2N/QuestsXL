package de.erethon.questsxl.objectives;

import de.fyreum.jobsxl.user.event.UserCraftItemEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.CraftItemEvent;

public class CraftObjective extends QBaseObjective {

    String id;

    @Override
    public void check(Event e) {
        if (e instanceof CraftItemEvent event) { // default crafting
            Player player = (Player) event.getWhoClicked();
            if (conditions(player) && event.getRecipe().getResult().getType().name().equalsIgnoreCase(id)) {
                complete(player, this);
            }
        } else if (e instanceof UserCraftItemEvent event) { // job crafting
            Player player = event.getUser().getPlayer();
            if (conditions(player) && event.getResult().getId().equalsIgnoreCase(id)) {
                complete(player, this);
            }
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        id = section.getString("id");
    }
}
