package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "remove_mob",
        description = "Removes one or multiple mobs from the world.",
        shortExample = "remove_mob: mob=example_mob; radius=32",
        longExample = {
                "remove_mob:",
                "  mob: example_mob",
                "  radius: 32",
                "  doDamage: true"
        }
)
public class RemoveMobAction extends QBaseAction {

    @QParamDoc(name = "mob", description = "The ID of the mob to remove", required = true)
    private String mob;
    @QParamDoc(name = "radius", description = "The radius in which to remove the mob", def = "32")
    private int radius;
    @QParamDoc(name = "doDamage", description = "Whether the mob should take damage and die, or just be removed", def = "false")
    private boolean doDamage;

    @Override
    public void play(QPlayer player) {
        super.play(player);
    }

    @Override
    public void play(QEvent event) {
        super.play(event);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        mob = cfg.getString("mob");
        radius = cfg.getInt("radius", 32);
        doDamage = cfg.getBoolean("doDamage", false);
    }
}
