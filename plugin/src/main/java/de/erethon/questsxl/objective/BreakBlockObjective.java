package de.erethon.questsxl.objective;

import de.erethon.hephaestus.blocks.HBlockLibrary;
import de.erethon.hephaestus.items.HItem;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import net.minecraft.resources.Identifier;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashSet;
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
public class BreakBlockObjective extends QBaseObjective<BlockBreakEvent> {

    private final HBlockLibrary blockLibrary = plugin.getBlockLibrary();

    @QParamDoc(name = "block", description = "The Hephaestus ID(s) of the block(s) that need to be broken (comma-separated for multiple types).", required = true)
    private final Set<Identifier> blockIds = new HashSet<>();

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        String blockText = blockIds.isEmpty() ? "blocks" : String.join(",", blockIds.stream().map(Identifier::toString).toArray(String[]::new));
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
        HItem item = blockLibrary.getItem(event.getBlock().getBlockData());
        if (item == null || blockIds.isEmpty()) {
            return;
        }

        if (blockIds.contains(item.getKey())) {
            complete(getPlayerHolder(event.getPlayer()), this, getPlayerHolder(event.getPlayer()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String blockStr = cfg.getString("block");
        if (blockStr == null || blockStr.trim().isEmpty()) {
            return;
        }

        String[] blocks = blockStr.split(",");
        for (String block : blocks) {
            String trimmedBlock = block.trim();
            try {
                Identifier blockId = Identifier.parse(trimmedBlock);
                blockIds.add(blockId);
            } catch (Exception e) {
                // Handle invalid resource location format
            }
        }
    }
}
