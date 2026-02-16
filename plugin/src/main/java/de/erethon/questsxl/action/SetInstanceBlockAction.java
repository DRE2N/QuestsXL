package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.instancing.InstanceManager;
import de.erethon.questsxl.instancing.InstancedArea;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;

import java.util.Optional;

@QLoadableDoc(
        value = "set_instance_block",
        description = "Sets a block at a specific position within the player's active instance.",
        shortExample = "set_instance_block: location=100,64,200 block=minecraft:stone",
        longExample = {
                "set_instance_block:",
                "  location: 100,64,200",
                "  block: minecraft:stone"
        }
)
public class SetInstanceBlockAction extends QBaseAction {

    private final InstanceManager instanceManager = QuestsXL.get().getInstanceManager();

    @QParamDoc(name = "location", description = "The block position (x,y,z)", required = true)
    private BlockPos blockPos;

    @QParamDoc(name = "block", description = "The block type (e.g., minecraft:stone)", required = true)
    private BlockState blockState;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;

        if (instanceManager == null || blockPos == null || blockState == null) {
            onFinish(quester);
            return;
        }

        if (quester instanceof QPlayer player) {
            InstancedArea instance = instanceManager.getActiveInstance(player);
            if (instance != null) {
                instanceManager.setBlock(instance, blockPos, blockState);
            }
        } else if (quester instanceof QEvent event) {
            // Set block in the first player's instance
            for (QPlayer player : event.getPlayersInRange()) {
                InstancedArea instance = instanceManager.getActiveInstance(player);
                if (instance != null) {
                    instanceManager.setBlock(instance, blockPos, blockState);
                    break;
                }
            }
        }

        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);

        String locationStr = cfg.getString("location");
        if (locationStr != null) {
            String[] parts = locationStr.split(",");
            if (parts.length == 3) {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());
                blockPos = new BlockPos(x, y, z);
            }
        }

        if (blockPos == null) {
            throw new RuntimeException("set_instance_block requires valid 'location' parameter (x,y,z)");
        }

        String blockStr = cfg.getString("block");
        if (blockStr != null) {
            Identifier resourceLocation = Identifier.tryParse(blockStr);
            if (resourceLocation != null) {
                Optional<Holder.Reference<Block>> block = BuiltInRegistries.BLOCK.get(resourceLocation);
                block.ifPresent(blockReference -> blockState = blockReference.value().defaultBlockState());
            }
        }

        if (blockState == null) {
            throw new RuntimeException("set_instance_block requires valid 'block' parameter");
        }
    }
}

