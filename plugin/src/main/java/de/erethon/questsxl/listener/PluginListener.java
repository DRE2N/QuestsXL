package de.erethon.questsxl.listener;

import de.erethon.questsxl.QuestsXL;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class PluginListener implements Listener {

    @EventHandler
    private void onServerLoad(ServerLoadEvent event) {
        QuestsXL.get().loadCore();

    }

}
