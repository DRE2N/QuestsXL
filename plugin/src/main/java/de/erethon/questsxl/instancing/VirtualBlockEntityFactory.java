package de.erethon.questsxl.instancing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Factory for creating virtual block entities from NBT data.
 */
public class VirtualBlockEntityFactory {

    /**
     * Block entity types that represent containers and should be virtualized.
     */
    private static final Set<BlockEntityType<?>> CONTAINER_TYPES = Set.of(
            BlockEntityType.CHEST,
            BlockEntityType.TRAPPED_CHEST,
            BlockEntityType.BARREL,
            BlockEntityType.SHULKER_BOX,
            BlockEntityType.FURNACE,
            BlockEntityType.BLAST_FURNACE,
            BlockEntityType.SMOKER,
            BlockEntityType.HOPPER,
            BlockEntityType.DISPENSER,
            BlockEntityType.DROPPER,
            BlockEntityType.BREWING_STAND
    );

    /**
     * Creates a VirtualBlockEntity from NBT data.
     *
     * @param pos The block position
     * @param nbt The NBT data containing block entity information
     * @return A new VirtualBlockEntity, or null if the type is not supported
     */
    public static VirtualBlockEntity fromNbt(BlockPos pos, CompoundTag nbt) {
        de.erethon.questsxl.QuestsXL.log("[VirtualBlockEntityFactory] fromNbt called, nbt keys: " + nbt.keySet());

        String typeId = nbt.getString("id").orElse("");
        de.erethon.questsxl.QuestsXL.log("[VirtualBlockEntityFactory] typeId from nbt: '" + typeId + "'");

        BlockEntityType<?> type = null;

        if (!typeId.isEmpty()) {
            Identifier resourceLocation = Identifier.tryParse(typeId);
            if (resourceLocation != null) {
                type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(resourceLocation);
            }
        }

        // Fallback: try to determine type from NBT contents
        if (type == null) {
            type = guessTypeFromNbt(nbt);
            de.erethon.questsxl.QuestsXL.log("[VirtualBlockEntityFactory] Guessed type from NBT: " + type);
        }

        de.erethon.questsxl.QuestsXL.log("[VirtualBlockEntityFactory] BlockEntityType: " + type);
        de.erethon.questsxl.QuestsXL.log("[VirtualBlockEntityFactory] Is container type: " + (type != null && CONTAINER_TYPES.contains(type)));

        if (type == null || !CONTAINER_TYPES.contains(type)) {
            return null; // Only virtualize containers
        }

        return new VirtualBlockEntity(pos, type, nbt);
    }

    /**
     * Tries to guess the block entity type from NBT contents.
     * Used as fallback for templates captured without the ID field.
     */
    private static BlockEntityType<?> guessTypeFromNbt(CompoundTag nbt) {
        // Check for common container-specific tags
        if (nbt.contains("Items")) {
            // Has Items tag - could be chest, barrel, hopper, etc.
            // Check for furnace-specific tags
            if (nbt.contains("BurnTime") || nbt.contains("CookTime")) {
                return BlockEntityType.FURNACE;
            }
            if (nbt.contains("BrewTime")) {
                return BlockEntityType.BREWING_STAND;
            }
            if (nbt.contains("TransferCooldown")) {
                return BlockEntityType.HOPPER;
            }
            // Default to chest for generic container with Items
            return BlockEntityType.CHEST;
        }
        return null;
    }

    /**
     * Creates an empty VirtualBlockEntity for a block state if it's a container.
     *
     * @param pos The block position
     * @param state The block state
     * @return A new VirtualBlockEntity with empty inventory, or null if not a container
     */
    public static VirtualBlockEntity createForBlockState(BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        de.erethon.questsxl.QuestsXL.log("[VirtualBlockEntityFactory] createForBlockState: block=" + block.getClass().getSimpleName());

        BlockEntityType<?> type = getBlockEntityTypeForBlock(block);
        de.erethon.questsxl.QuestsXL.log("[VirtualBlockEntityFactory] BlockEntityType=" + (type != null ? type : "null"));

        if (type == null) {
            return null;
        }

        // Create empty NBT data for this block entity
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).toString());
        nbt.putInt("x", pos.getX());
        nbt.putInt("y", pos.getY());
        nbt.putInt("z", pos.getZ());

        return new VirtualBlockEntity(pos, type, nbt);
    }

    /**
     * Gets the BlockEntityType for a given block if it's a container.
     */
    private static BlockEntityType<?> getBlockEntityTypeForBlock(Block block) {
        if (block instanceof ChestBlock) {
            return BlockEntityType.CHEST;
        } else if (block instanceof BarrelBlock) {
            return BlockEntityType.BARREL;
        } else if (block instanceof ShulkerBoxBlock) {
            return BlockEntityType.SHULKER_BOX;
        } else if (block instanceof AbstractFurnaceBlock) {
            // Determine specific furnace type
            if (block == net.minecraft.world.level.block.Blocks.FURNACE) {
                return BlockEntityType.FURNACE;
            } else if (block == net.minecraft.world.level.block.Blocks.BLAST_FURNACE) {
                return BlockEntityType.BLAST_FURNACE;
            } else if (block == net.minecraft.world.level.block.Blocks.SMOKER) {
                return BlockEntityType.SMOKER;
            }
            return BlockEntityType.FURNACE; // Default
        } else if (block instanceof HopperBlock) {
            return BlockEntityType.HOPPER;
        } else if (block instanceof DispenserBlock) {
            return BlockEntityType.DISPENSER;
        } else if (block instanceof DropperBlock) {
            return BlockEntityType.DROPPER;
        } else if (block instanceof BrewingStandBlock) {
            return BlockEntityType.BREWING_STAND;
        }
        return null;
    }

    /**
     * Checks if a block entity type should be virtualized.
     */
    public static boolean isContainerType(BlockEntityType<?> type) {
        return CONTAINER_TYPES.contains(type);
    }

    /**
     * Checks if a block entity type should be virtualized by its ID.
     */
    public static boolean isContainerType(String typeId) {
        Identifier resourceLocation = Identifier.tryParse(typeId);
        if (resourceLocation == null) {
            return false;
        }
        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(resourceLocation);
        return type != null && CONTAINER_TYPES.contains(type);
    }
}
