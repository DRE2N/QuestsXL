package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.instancing.InstanceManager;
import de.erethon.questsxl.instancing.InstancedArea;
import de.erethon.questsxl.player.QPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

@QLoadableDoc(
        value = "instance_block",
        description = "Checks if a block at a specific position in the player's instance matches the specified type.",
        shortExample = "instance_block: location=100,64,200 block=minecraft:air",
        longExample = {
                "instance_block:",
                "  location: 100,64,200",
                "  block: minecraft:air"
        }
)
public class InstanceBlockCondition extends QBaseCondition {

    private final InstanceManager instanceManager = QuestsXL.get().getInstanceManager();

    @QParamDoc(name = "location", description = "The block position (x,y,z)", required = true)
    private BlockPos blockPos;

    @QParamDoc(name = "block", description = "The expected block type (e.g., minecraft:air)", required = true)
    private Optional<Holder.Reference<Block>> expectedBlock;

    @Override
    public boolean check(Quester quester) {
        if (instanceManager == null || blockPos == null || expectedBlock == null) {
            return false;
        }

        if (!(quester instanceof QPlayer player)) {
            return false;
        }

        InstancedArea instance = instanceManager.getActiveInstance(player);
        if (instance == null) {
            return false;
        }

        BlockState state = instance.getEffectiveBlockState(blockPos);
        if (state == null) {
            return false;
        }

        return state.getBlock() == expectedBlock.get().value();
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
            throw new RuntimeException("instance_block condition requires valid 'location' parameter (x,y,z)");
        }

        String blockStr = cfg.getString("block");
        if (blockStr != null) {
            Identifier resourceLocation = Identifier.tryParse(blockStr);
            if (resourceLocation != null) {
                expectedBlock = BuiltInRegistries.BLOCK.get(resourceLocation);
            }
        }

        if (expectedBlock.isEmpty()) {
            throw new RuntimeException("instance_block condition requires valid 'block' parameter");
        }
    }
}

