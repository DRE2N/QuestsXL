package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.instancing.BlockCollectionManager;
import de.erethon.questsxl.instancing.InstancedBlockCollection;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class HideIBCAction extends QBaseAction {

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
    public void load(QConfig cfg) {
        collection = manager.getByID(cfg.getString("id"));
        if (collection == null) {
            throw new RuntimeException("Collection " + cfg.getString("id") + " does not exist.");
        }
    }
}

