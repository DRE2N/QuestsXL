package de.erethon.questsxl.component.objective;

import de.erethon.hephaestus.blocks.HBlockLibrary;
import de.erethon.hephaestus.items.HItem;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import net.minecraft.resources.Identifier;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@QLoadableDoc(
        value = "break_block",
        description = "Completed when a specific block is broken.",
        shortExample = "break_block: block=erethon:fancy_block,minecraft:stone,minecraft:dirt",
        longExample = {
                "break_block:",
                "  block: 'erethon:fancy_block,minecraft:stone'"
        }
)
public class BreakBlockObjective extends QBaseObjective<BlockBreakEvent> implements VariableProvider {

    private final HBlockLibrary blockLibrary = plugin.getBlockLibrary();

    @QParamDoc(name = "block", description = "The Hephaestus ID(s) of the block(s) that need to be broken (comma-separated for multiple types).", required = true)
    private final Set<Identifier> blockIds = new HashSet<>();

    private int lastProgress = 0;

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        String blockText = blockIds.isEmpty() ? "blocks" : String.join(", ", blockIds.stream().map(Identifier::toString).toArray(String[]::new));
        return QTranslatable.fromString("de=Baue " + blockText + " ab; en=Break " + blockText);
    }

    @Override
    public Class<BlockBreakEvent> getEventType() {
        return BlockBreakEvent.class;
    }

    @Override
    public void check(ActiveObjective activeObjective, BlockBreakEvent event) {
        if (!conditions(event.getPlayer())) {
            return;
        }
        if (blockIds.isEmpty()) {
            return;
        }

        // Try to resolve the block as a custom Hephaestus block first,
        // fall back to the vanilla material's namespaced key for plain vanilla blocks.
        Identifier blockKey;
        HItem item = blockLibrary.getItem(event.getBlock().getBlockData());
        if (item != null) {
            blockKey = item.getKey();
        } else {
            NamespacedKey nk = event.getBlock().getType().getKey();
            blockKey = Identifier.parse(nk.getNamespace() + ":" + nk.getKey());
        }

        if (blockIds.contains(blockKey)) {
            lastProgress = activeObjective.getProgress() + 1;
            checkCompletion(activeObjective, this, plugin.getDatabaseManager().getCurrentPlayer(event.getPlayer()));
        }
    }

    /** Exposes %progress% and %goal% to child actions (onComplete / onProgress). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of(
                "progress", new QVariable(lastProgress),
                "goal", new QVariable(progressGoal)
        );
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String blockStr = cfg.getString("block");
        if (blockStr == null || blockStr.trim().isEmpty()) {
            return;
        }

        for (String block : blockStr.split(",")) {
            String trimmed = block.trim();
            try {
                blockIds.add(Identifier.parse(trimmed));
            } catch (Exception e) {
                // Handle invalid resource location format
            }
        }
    }
}
