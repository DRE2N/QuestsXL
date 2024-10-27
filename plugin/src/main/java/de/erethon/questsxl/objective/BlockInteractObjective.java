package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;

@QLoadableDoc(
        value = "block_interact",
        description = "This objective is completed when a player interacts with a block at the specified location.",
        shortExample = "block_interact: x=64; y=64; z=64; world=world",
        longExample = {
                "block_interact:",
                "  x: 64",
                "  y: 64",
                "  z: 64",
                "  world: world"
        }
)
public class BlockInteractObjective extends QBaseObjective {

    @QParamDoc(name = "location", description = "The location of the block that the player must interact with. QLocation", required = true)
    private QLocation location;

    @Override
    public void check(ActiveObjective active, Event event) {
        if (!(event instanceof PlayerInteractEvent e)) return;
        if (!conditions(e.getPlayer())) return;
        if (e.getClickedBlock() == null) return;
        if (location.equals(e.getClickedBlock().getLocation())) {
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(e.getPlayer()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        location = cfg.getQLocation("location");
    }
}
