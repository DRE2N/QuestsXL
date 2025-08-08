package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.instancing.BlockCollectionManager;
import de.erethon.questsxl.instancing.InstancedBlockCollection;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "reset_ibc",
        description = "Resets an InstancedBlockCollection for a player (restores real, shared world state).",
        shortExample = "reset_ibc: ibc=example_collection",
        longExample = {
                "reset_ibc:",
                "  ibc: example_collection"
        }
)
public class ResetIBCAction extends QBaseAction {

    BlockCollectionManager manager = QuestsXL.get().getBlockCollectionManager();

    @QParamDoc(name = "ibc", description = "The ID of the IBC to reset", required = true)
    InstancedBlockCollection collection = null;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester,  (QPlayer player) -> collection.reset(player.getPlayer()));
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        collection = manager.getByID(cfg.getString("ibc"));
        if (collection == null) {
            throw new RuntimeException("Collection " + cfg.getString("ibc") + " does not exist.");
        }
    }

}
