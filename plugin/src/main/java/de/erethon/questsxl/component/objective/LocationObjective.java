package de.erethon.questsxl.component.objective;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.script.QLocation;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.livingworld.ContentGuide;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;

@QLoadableDoc(
        value = "location",
        description = "This objective is completed when a player reaches a specific location. Can be cancelled, preventing the player from moving closer",
        shortExample = "location: x=64; y=64; z=64; range=5",
        longExample = {
                "location: # This is completed when the player moves five blocks up from their current location.",
                "  x: ~0",
                "  y: ~5",
                "  z: ~0"
        }
)
public class LocationObjective extends QBaseObjective<PlayerMoveEvent> implements VariableProvider {

    @QParamDoc(name = "location", description = "The location that the player must reach. QLocation", required = true)
    private QLocation loc;
    @QParamDoc(name = "range", description = "How close the player has to get to the location", def = "1")
    private int range;

    private double lastDistance = 0;

    public QLocation getLocation() {
        return loc;
    }

    @Override
    public void check(ActiveObjective active, PlayerMoveEvent e) {
        if (!conditions(e.getPlayer())) return;
        if (loc == null || !loc.sameWorld(e.getTo())) {
            return;
        }
        Location targetLocation = loc.get(e.getTo());
        lastDistance = e.getTo().distance(targetLocation);
        if (lastDistance <= range) {
            if (shouldCancelEvent) e.setCancelled(true);
            checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));
        }
    }

    /**
     * Exposes %target_x%, %target_y%, %target_z%, %distance% to child actions
     * (onComplete, onProgress, onFail).
     */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        Map<String, QVariable> vars = new HashMap<>();
        vars.put("distance", new QVariable(lastDistance));
        if (loc != null) {
            Location resolved = loc.get(quester.getLocation());
            vars.put("target_x", new QVariable(resolved.getBlockX()));
            vars.put("target_y", new QVariable(resolved.getBlockY()));
            vars.put("target_z", new QVariable(resolved.getBlockZ()));
        }
        return vars;
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        loc = cfg.getQLocation("location");
        range = cfg.getInt("range", 1);
        if (range <= 0) {
            throw new RuntimeException("The location objective in " + cfg.getName() + " contains a negative range.");
        }
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        if (loc != null) {
            String markerText = "[" + ContentGuide.getDirectionalMarker(player, loc.get(player.getLocation())) + "] ";
            String locationText = String.format("%d / %d / %d",
                    (int) loc.getX(), (int) loc.getY(), (int) loc.getZ());
            return QTranslatable.fromString("en=" + markerText + "Go to " + locationText + "; de=" + markerText + "Gehe zu " + locationText);
        }
        return QTranslatable.fromString("en=Go to location; de=Gehe zu einem Ort");
    }

    @Override
    public Class<PlayerMoveEvent> getEventType() {
        return PlayerMoveEvent.class;
    }
}
