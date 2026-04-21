package de.erethon.questsxl.component.action;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.Quester;

@QLoadableDoc(
        value = "dummy",
        description = "This action does nothing. It is used for testing and debugging.",
        shortExample = "dummy:"
)
public class DummyAction extends QBaseAction {

    @Override
    public void playInternal(Quester quester) {
    }


    @Override
    public void delayedEnd(int seconds) {
    }

    @Override
    public void cancel() {
    }

    @Override
    public String getID() {
        return "DUMMY";
    }

    @Override
    public void load(QConfig cfg) {
    }
}
