package de.erethon.questsxl.condition;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

@QLoadableDoc(
        value = "inverted",
        description = "This condition is successful if all of its conditions are not successful.",
        shortExample = "<no short syntax>",
        longExample = {
                "inverted:",
                "  conditions:",
                "  - event_state: id=example; state=disabled",
        }
)
public class InvertedCondition extends QBaseCondition {

    @QParamDoc(name = "conditions", description = "A list of conditions that must not be successful for this condition to be successful.", required = true)
    Set<QCondition> conditions = new HashSet<>();

    @Override
    public boolean check(QPlayer player) {
        for (QCondition condition : conditions) {
            if (!condition.check(player)) {
                return success(player);
            }
        }
        return fail(player);
    }

    @Override
    public boolean check(QEvent event) {
        for (QCondition condition : conditions) {
            if (!condition.check(event)) {
                return success(event);
            }
        }
        return fail(event);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        conditions = cfg.getConditions("conditions");
    }

}
