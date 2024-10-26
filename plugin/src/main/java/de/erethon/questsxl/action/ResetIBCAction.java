package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.instancing.BlockCollectionManager;
import de.erethon.questsxl.instancing.InstancedBlockCollection;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "reset_ibc",
        description = "Resets an InstancedBlockCollection for a player (restores real, shared world state).",
        shortExample = "- 'reset_ibc: id=example_collection'",
        longExample = {
                "reset_ibc:",
                "  id: example_collection"
        }
)
public class ResetIBCAction extends QBaseAction {

    BlockCollectionManager manager = QuestsXL.getInstance().getBlockCollectionManager();

    @QParamDoc(name = "id", description = "The ID of the IBC to reset", required = true)
    InstancedBlockCollection collection = null;

    @Override
    public void play(QPlayer player) {
        collection.reset(player.getPlayer());
    }

    @Override
    public void load(QConfig cfg) {
        collection = manager.getByID(cfg.getString("id"));
        if (collection == null) {
            throw new RuntimeException("Collection " + cfg.getString("id") + " does not exist.");
        }
    }

}
