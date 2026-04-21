package de.erethon.questsxl.component.objective;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.script.QLocation;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;

@QLoadableDoc(
        value = "block_interact",
        description = "This objective is completed when a player interacts with a block at the specified location. Can be cancelled.",
        shortExample = "block_interact: x=64; y=64; z=64; world=world",
        longExample = {
                "block_interact:",
                "  x: 64",
                "  y: 64",
                "  z: 64",
                "  world: world"
        }
)
public class BlockInteractObjective extends QBaseObjective<PlayerInteractEvent> implements VariableProvider {

    @QParamDoc(name = "location", description = "The location of the block that the player must interact with. QLocation", required = true)
    private QLocation location;

    private int lastBlockX = 0, lastBlockY = 0, lastBlockZ = 0;

    @Override
    public void check(ActiveObjective active, PlayerInteractEvent e) {
        if (!conditions(e.getPlayer())) return;
        if (e.getClickedBlock() == null) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (location.get(e.getClickedBlock().getLocation()).equals(e.getClickedBlock().getLocation())) {
            lastBlockX = e.getClickedBlock().getX();
            lastBlockY = e.getClickedBlock().getY();
            lastBlockZ = e.getClickedBlock().getZ();
            if (shouldCancelEvent) e.setCancelled(true);
            checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));
        }
    }

    /** Exposes %block_x%, %block_y%, %block_z% to child actions (onComplete). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        Map<String, QVariable> vars = new HashMap<>();
        vars.put("block_x", new QVariable(lastBlockX));
        vars.put("block_y", new QVariable(lastBlockY));
        vars.put("block_z", new QVariable(lastBlockZ));
        return vars;
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        location = cfg.getQLocation("location");
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        String locationText = location != null ? location.toString() : "a specific location";
        return QTranslatable.fromString("en=Interact with block at " + locationText + "; de=Interagiere mit dem Block bei " + locationText);
    }

    @Override
    public Class<PlayerInteractEvent> getEventType() {
        return PlayerInteractEvent.class;
    }
}
