package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.Quester;

public class ItemInHandCondition extends QBaseCondition {
    @Override
    public boolean checkInternal(Quester quester) {
        return false;
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
    }
}
