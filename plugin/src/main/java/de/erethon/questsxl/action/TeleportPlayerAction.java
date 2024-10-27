package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;

@QLoadableDoc(
        value = "teleport",
        description = "Teleports the player to a location",
        shortExample = "- 'teleport: x=~0; y=~1; z=~0;' # Teleports the player one block up",
        longExample = {
                "teleport:",
                "  target:",
                "    x: 0",
                "    y: 64",
                "    z: 0",
        }
)
public class TeleportPlayerAction extends QBaseAction{

    @QParamDoc(name = "target", description = "The location to teleport the player to", required = true)
    QLocation target;

    public void play(QPlayer player) {
        if (!conditions(player)) return;
        player.getPlayer().teleport(target.get(player.getPlayer().getLocation()));
        onFinish(player);
    }

    public Material getIcon() {
        return Material.ENDER_PEARL;
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        target = cfg.getQLocation("target");
    }
}
