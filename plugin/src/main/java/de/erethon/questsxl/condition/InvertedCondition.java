package de.erethon.questsxl.condition;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.Quester;
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
    public boolean check(Quester quester) {
        for (QCondition condition : conditions) {
            if (!condition.check(quester)) {
                return success(quester);
            }
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        conditions = cfg.getConditions(this, "conditions");
    }

}
