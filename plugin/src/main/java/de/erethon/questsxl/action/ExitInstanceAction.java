package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.instancing.InstanceManager;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "exit_instance",
        description = "Removes the player(s) from their current instanced area and returns them to the real world view.",
        shortExample = "exit_instance:",
        longExample = {
                "exit_instance:"
        }
)
public class ExitInstanceAction extends QBaseAction {

    private final InstanceManager instanceManager = QuestsXL.get().getInstanceManager();

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;

        if (instanceManager == null) {
            onFinish(quester);
            return;
        }

        if (quester instanceof QPlayer player) {
            instanceManager.leaveInstance(player);
        } else if (quester instanceof QEvent event) {
            for (QPlayer player : event.getPlayersInRange()) {
                instanceManager.leaveInstance(player);
            }
        }

        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
    }
}

