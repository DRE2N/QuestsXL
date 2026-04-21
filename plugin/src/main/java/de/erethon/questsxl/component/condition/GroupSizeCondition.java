package de.erethon.questsxl.component.condition;

import de.erethon.aergia.Aergia;
import de.erethon.aergia.group.Group;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.player.QPlayer;

import java.util.Map;

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
public class GroupSizeCondition extends QBaseCondition implements VariableProvider {

    @QParamDoc(name = "min", description = "The minimum group size.", def = "1")
    int min;
    @QParamDoc(name = "max", description = "The maximum group size.", def = "5")
    int max;

    private int lastGroupSize = 0;

    @Override
    protected boolean checkInternal(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return fail(quester);
        }
        Group group = Aergia.inst().getGroupManager().getGroup(player.getPlayer());
        if (group != null) {
            lastGroupSize = group.getSize();
            if (lastGroupSize >= min && lastGroupSize <= max) {
                return success(quester);
            }
        } else {
            lastGroupSize = 0;
        }
        return fail(quester);
    }

    /**
     * Exposes %group_size% to child actions (onSuccess / onFail).
     */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("group_size", new QVariable(lastGroupSize));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        min = cfg.getInt("min", 1);
        max = cfg.getInt("max", 5);
    }
}
