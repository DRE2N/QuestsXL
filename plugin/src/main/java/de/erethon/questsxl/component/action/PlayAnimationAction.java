package de.erethon.questsxl.component.action;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.Quester;

@QLoadableDoc(
        value = "play_animation",
        description = "**Currently not implemented**"
)
public class PlayAnimationAction extends QBaseAction {

    @Override
    public void playInternal(Quester quester) {
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
    }
}
