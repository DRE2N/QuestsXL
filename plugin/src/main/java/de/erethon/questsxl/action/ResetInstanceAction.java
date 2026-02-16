package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.instancing.InstanceManager;
import de.erethon.questsxl.instancing.InstancedArea;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "reset_instance",
        description = "Resets an instance to its template state, clearing all modifications.",
        shortExample = "reset_instance:",
        longExample = {
                "reset_instance:"
        }
)
public class ResetInstanceAction extends QBaseAction {

    private final InstanceManager instanceManager = QuestsXL.get().getInstanceManager();

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;

        if (instanceManager == null) {
            onFinish(quester);
            return;
        }

        if (quester instanceof QPlayer player) {
            InstancedArea instance = instanceManager.getActiveInstance(player);
            if (instance != null) {
                instanceManager.resetInstance(instance);
            }
        } else if (quester instanceof QEvent event) {
            // Reset instance for first player found (they likely all share the same instance)
            for (QPlayer player : event.getPlayersInRange()) {
                InstancedArea instance = instanceManager.getActiveInstance(player);
                if (instance != null) {
                    instanceManager.resetInstance(instance);
                    break; // Only reset once
                }
            }
        }

        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
    }
}

