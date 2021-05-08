package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.instancing.BlockCollectionManager;
import de.erethon.questsxl.instancing.InstancedBlockCollection;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class ShowIBC extends QBaseAction {

    BlockCollectionManager manager = QuestsXL.getInstance().getBlockCollectionManager();
    InstancedBlockCollection collection = null;

    @Override
    public void play(Player player) {
        collection.show(player);
    }

    @Override
    public void load(String[] msg) {
        collection = manager.getByID(msg[0]);
    }

    @Override
    public void load(ConfigurationSection section) {
        collection = manager.getByID(section.getString("id"));
    }
}
