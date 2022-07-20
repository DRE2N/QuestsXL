package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.instancing.BlockCollectionManager;
import de.erethon.questsxl.instancing.InstancedBlockCollection;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class HideIBC extends QBaseAction {

    BlockCollectionManager manager = QuestsXL.getInstance().getBlockCollectionManager();
    InstancedBlockCollection collection = null;

    @Override
    public void play(QPlayer player) {
        collection.hide(player.getPlayer());
    }

    @Override
    public void play(QEvent event) {

    }

    @Override
    public void load(String[] msg) {
        collection = manager.getByID(msg[0]);
        if (collection == null) {
            throw new RuntimeException("Collection " + msg[0] + " does not exist.");
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        collection = manager.getByID(section.getString("id"));
        if (collection == null) {
            throw new RuntimeException("Collection " + section.getString("id") + " does not exist.");
        }
    }
}

