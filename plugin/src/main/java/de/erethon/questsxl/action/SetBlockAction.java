package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Location;
import org.bukkit.Material;

@QLoadableDoc(
        value = "set_block",
        description = "Sets a block at a specific location to a specific material.",
        shortExample = "set_block: location: x=0; y=64; z=0; material=STONE",
        longExample = {
                "set_block:",
                "  location:",
                "    x: 0",
                "    y: 64",
                "    z: 0",
                "    world: Erethon",
                "  material: DIAMOND_BLOCK",
                "  instanced: true # "
        }
)
public class SetBlockAction extends QBaseAction {

    @QParamDoc(name = "location", description = "The location where the block will be set", required = true)
    private QLocation location;
    @QParamDoc(name = "material", description = "The material to set the block to", required = true)
    private Material material;
    @QParamDoc(name = "instanced", description = "If true, only the Quester will see the block change.", def = "false")
    private boolean instanced = false;

    public void play(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester, (QPlayer player) -> {
            Location loc = location.get(player.getLocation());
            if (!instanced) {
                if (!loc.isChunkLoaded()) return;
                loc.getBlock().setType(material);
            } else {
                player.getPlayer().sendBlockChange(loc, material.createBlockData());
            }
        });
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        location = cfg.getQLocation("location", null);
        material = Material.getMaterial(cfg.getString("material", null).toUpperCase());
        instanced = cfg.getBoolean("instanced", false);
        if (material == null) {
            QuestsXL.get().addRuntimeError(new FriendlyError(id(), "Material is missing"));
        }
        if (location == null) {
            QuestsXL.get().addRuntimeError(new FriendlyError(id(), "Location is missing"));
        }
    }
}
