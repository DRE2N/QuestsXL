package de.erethon.questsxl.listener;

import de.fyreum.jobsxl.user.event.UserCraftItemEvent;
import de.fyreum.jobsxl.user.event.UserGainJobExperienceEvent;
import org.bukkit.event.EventHandler;

/**
 * @author Fyreum
 */
public class PlayerJobListener extends AbstractListener {

    @EventHandler
    public void onJobCraft(UserCraftItemEvent event) {
        checkObjectives(event.getUser().getPlayer(), event);
    }

    @EventHandler
    public void onJobExpGain(UserGainJobExperienceEvent event) {
        checkObjectives(event.getUser().getPlayer(), event);
    }

}
