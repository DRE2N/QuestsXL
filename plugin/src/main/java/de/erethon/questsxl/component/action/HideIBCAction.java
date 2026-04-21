package de.erethon.questsxl.component.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.instancing.BlockCollectionManager;
import de.erethon.questsxl.livingworld.instancing.InstancedBlockCollection;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "hide_ibc",
        description = "Hides an InstancedBlockCollection from a player.",
        shortExample = "hide_ibc: id=example_collection",
        longExample = {
                "hide_ibc:",
                "  id: example_collection"
        }
)
public class HideIBCAction extends QBaseAction {

    BlockCollectionManager manager = QuestsXL.get().getBlockCollectionManager();
    @QParamDoc(name = "id", description = "The ID of the IBC to hide", required = true)
    InstancedBlockCollection collection = null;

    @Override
    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester,(QPlayer player) -> collection.hide(player.getPlayer()));
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        collection = manager.getByID(cfg.getString("id"));
        if (collection == null) {
            throw new RuntimeException("Collection " + cfg.getString("id") + " does not exist.");
        }
    }
}

