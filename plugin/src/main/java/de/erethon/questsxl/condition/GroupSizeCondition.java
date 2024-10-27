package de.erethon.questsxl.condition;

import de.erethon.aergia.Aergia;
import de.erethon.aergia.group.Group;
import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "group_size",
        description = "Checks if the player's current group is between min and max group size. Requires Aergia to be installed.",
        shortExample = "group_size: min=2; max=5",
        longExample = {
                "group_size:",
                "  min: 2",
                "  max: 5",
        }
)
public class GroupSizeCondition extends QBaseCondition {

    @QParamDoc(name = "min", description = "The minimum group size.", def = "1")
    int min;
    @QParamDoc(name = "max", description = "The maximum group size.", def = "5")
    int max;

    @Override
    public boolean check(QPlayer player) {
        Group group = Aergia.inst().getGroupManager().getGroup(player.getPlayer());
        if (group != null && group.getSize() >= min && group.getSize() <= max) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public boolean check(QEvent event) {
        return fail(event);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        min = cfg.getInt("min", 1);
        max = cfg.getInt("max", 5);
    }

}
