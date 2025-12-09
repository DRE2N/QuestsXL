package de.erethon.questsxl.objective;

import de.erethon.hephaestus.blocks.HBlockLibrary;
import de.erethon.hephaestus.items.HItem;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

@QLoadableDoc(
        value = "break_block",
        description = "Completed when a specific block is broken.",
        shortExample = "break_block: block=erethon:fancy_block",
        longExample = {
                "break_block:",
                "  block: 'erethon:fancy_block'"
        }
)
public class BreakBlockObjective extends QBaseObjective<BlockBreakEvent> {

    private final HBlockLibrary blockLibrary = plugin.getBlockLibrary();

    @QParamDoc(name = "block", description = "The Hephaestus ID of the block that needs to be broken.", required = true)
    private ResourceLocation blockId = null;

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("de=Baue " + blockId + " ab; en=Break " + blockId);
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
        if (item == null || blockId == null) {
            return;
        }

        if (item.getKey().equals(blockId)) {
            complete(getPlayerHolder(event.getPlayer()), this, getPlayerHolder(event.getPlayer()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        if (cfg.contains("block")) {
            blockId = ResourceLocation.parse(cfg.getString("block"));
        }

    }
}
