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
        value = "show_ibc",
        description = "Shows an Instanced Block Collection to the player.",
        shortExample = "show_ibc: ibc=example_collection",
        longExample = {
                "show_ibc:",
                "  ibc: example_collection"
        }
)
public class ShowIBCAction extends QBaseAction {

    BlockCollectionManager manager = QuestsXL.getInstance().getBlockCollectionManager();

    @QParamDoc(name = "ibc", description = "The ID of the collection to show", required = true)
    InstancedBlockCollection collection = null;

    @Override
    public void play(QPlayer player) {
        collection.show(player.getPlayer());
    }

    @Override
    public void load(QConfig cfg) {
        collection = manager.getByID(cfg.getString("ibc"));
        if (collection == null) {
            throw new RuntimeException("Collection " + cfg.getString("ibc") + " does not exist.");
        }
    }
}
