package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QTranslatable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class EntityInteractObjective extends QBaseObjective<PlayerInteractEntityEvent> {

    String mob;

    @Override
    public void check(ActiveObjective active, PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        if (!conditions(player)) {
            return;
        }
        Entity entity = e.getRightClicked();
        if (!entity.getType().name().equalsIgnoreCase(mob)) {
            /* Needs rework for new Aether
            ActiveNPC activeNPC = plugin.getAether().getActiveCreatureManager().get(entity.getUniqueId());
            if (activeNPC == null || !activeNPC.getNpc().getID().equalsIgnoreCase(mob)) {
                return;
            }*/
        }
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(player));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        mob = cfg.getString("id");
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Interact with " + mob + "; de=Interagiere mit " + mob);
    }

    @Override
    public Class<PlayerInteractEntityEvent> getEventType() {
        return PlayerInteractEntityEvent.class;
    }
}
