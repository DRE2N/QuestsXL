package de.erethon.questsxl.objectives;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

public class EntityInteractObjective extends QBaseObjective {

    UUID entityUUID;

    @Override
    public void check(Event e) {
        if (!(e instanceof PlayerInteractEntityEvent)) return;
        PlayerInteractEntityEvent event = (PlayerInteractEntityEvent) e;

        if (event.getRightClicked().getUniqueId().equals(entityUUID)) {
            complete(event.getPlayer(), this);
        }
    }

    @Override
    public String getDisplayText() {
        return "Interact with " + entityUUID;
    }



}
