package de.erethon.questsxl.component.objective;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashMap;
import java.util.Map;

public class EntityInteractObjective extends QBaseObjective<PlayerInteractEntityEvent> implements VariableProvider {

    String mob;

    private String lastEntityType = "";
    private String lastEntityName = "";

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
        lastEntityType = entity.getType().name();
        lastEntityName = entity.getName();
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(player));
    }

    /** Exposes %entity_type% and %entity_name% to child actions (onComplete). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        Map<String, QVariable> vars = new HashMap<>();
        vars.put("entity_type", new QVariable(lastEntityType));
        vars.put("entity_name", new QVariable(lastEntityName));
        return vars;
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
