package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;

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
    public boolean checkInternal(Quester quester) {
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
