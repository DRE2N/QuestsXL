package de.erethon.questsxl.component.objective;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.script.QLocation;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@QLoadableDoc(
        value = "block_interact",
        description = "This objective is completed when a player interacts with a block at the specified location. Can be cancelled. Optionally requires the player to look at the block for a set number of seconds; progress drains when they look away.",
        shortExample = "block_interact: x=64; y=64; z=64; world=world",
        longExample = {
                "block_interact:",
                "  x: 64",
                "  y: 64",
                "  z: 64",
                "  world: world",
                "  holdTime: 10",
                "  holdTitle: en=Defusing...; de=Entschärfe..."
        }
)
public class BlockInteractObjective extends QBaseObjective<PlayerInteractEvent> implements VariableProvider {

    @QParamDoc(name = "location", description = "The location of the block that the player must interact with. QLocation", required = true)
    private QLocation location;

    @QParamDoc(name = "holdTime", description = "Seconds the player must continuously look at the block. Progress drains when they look away. 0 = instant (default).")
    private int holdTime = 0;

    @QParamDoc(name = "holdTitle", description = "Title shown above the progress bar subtitle while holding. Supports QTranslatable format.")
    private QTranslatable holdTitle;

    private int lastBlockX = 0, lastBlockY = 0, lastBlockZ = 0;

    private static final TextColor FILLED_COLOR = TextColor.fromCSSHexString("#00aa00");
    private static final TextColor EMPTY_COLOR  = TextColor.fromCSSHexString("#404040");
    private static final Title.Times TITLE_TIMES =
            Title.Times.times(Duration.ZERO, Duration.ofMillis(200), Duration.ofMillis(400));
    private static final int BAR_WIDTH = 20;

    private final Map<ActiveObjective, Map<UUID, HoldState>> activeHolds = new HashMap<>();

    private record HoldState(BukkitRunnable task, Player player) {
        void cancel() {
            player.resetTitle();
            if (!task.isCancelled()) task.cancel();
        }
    }

    @Override
    public void check(ActiveObjective active, PlayerInteractEvent e) {
        if (!conditions(e.getPlayer())) return;
        if (e.getClickedBlock() == null) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Location blockLoc = location.get(e.getClickedBlock().getLocation());
        if (!blockLoc.equals(e.getClickedBlock().getLocation())) return;

        if (shouldCancelEvent) e.setCancelled(true);

        lastBlockX = blockLoc.getBlockX();
        lastBlockY = blockLoc.getBlockY();
        lastBlockZ = blockLoc.getBlockZ();

        if (holdTime <= 0) {
            checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));
            return;
        }

        Player player = e.getPlayer();
        Map<UUID, HoldState> holds = activeHolds.computeIfAbsent(active, k -> new HashMap<>());
        if (holds.containsKey(player.getUniqueId())) return;

        var qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        Component titleComponent = holdTitle != null
                ? GlobalTranslator.render(holdTitle.get(), player.locale())
                : Component.empty();
        titleComponent = titleComponent.shadowColor(ShadowColor.none());
        int totalTicks = holdTime * 20;
        Component finalTitleComponent = titleComponent;
        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()
                        || player.getLocation().getWorld() != blockLoc.getWorld()
                        || player.getLocation().distanceSquared(blockLoc) > 16.0) {
                    holds.remove(player.getUniqueId());
                    player.resetTitle();
                    cancel();
                    return;
                }

                Block target = player.getTargetBlockExact(5);
                boolean isLooking = target != null
                        && target.getX() == blockLoc.getBlockX()
                        && target.getY() == blockLoc.getBlockY()
                        && target.getZ() == blockLoc.getBlockZ();

                ticks = isLooking ? Math.min(ticks + 1, totalTicks) : Math.max(ticks - 1, 0);

                int filled = (int) ((double) ticks / totalTicks * BAR_WIDTH);
                Component filledSegment = Component.text(" ").color(FILLED_COLOR).decorate(TextDecoration.STRIKETHROUGH).shadowColor(ShadowColor.none());
                Component emptySegment  = Component.text(" ").color(EMPTY_COLOR).decorate(TextDecoration.STRIKETHROUGH).shadowColor(ShadowColor.none());
                Component bar = Component.empty();
                for (int i = 0; i < BAR_WIDTH; i++) {
                    bar = bar.append(i < filled ? filledSegment : emptySegment);
                }
                player.showTitle(Title.title(finalTitleComponent, bar, TITLE_TIMES));

                if (ticks >= totalTicks) {
                    holds.remove(player.getUniqueId());
                    player.resetTitle();
                    cancel();
                    checkCompletion(active, BlockInteractObjective.this, qPlayer);
                }
            }
        };
        holds.put(player.getUniqueId(), new HoldState(task, player));
        task.runTaskTimer(plugin, 1L, 1L);
    }

    @Override
    public void onStop(ActiveObjective active) {
        Map<UUID, HoldState> holds = activeHolds.remove(active);
        if (holds != null) {
            holds.values().forEach(HoldState::cancel);
        }
    }

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
        holdTime = cfg.getInt("holdTime", 0);
        if (cfg.contains("holdTitle")) {
            holdTitle = QTranslatable.fromString(cfg.getString("holdTitle"));
        }
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        String locationText = location != null ? location.toString() : "a specific location";
        if (holdTime > 0) {
            return QTranslatable.fromString(
                    "en=Hold block at " + locationText + " for " + holdTime + "s" +
                    "; de=Block bei " + locationText + " für " + holdTime + "s halten");
        }
        return QTranslatable.fromString("en=Interact with block at " + locationText + "; de=Interagiere mit dem Block bei " + locationText);
    }

    @Override
    public Class<PlayerInteractEvent> getEventType() {
        return PlayerInteractEvent.class;
    }
}
