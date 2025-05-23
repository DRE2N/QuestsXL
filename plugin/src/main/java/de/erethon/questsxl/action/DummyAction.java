package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "dummy",
        description = "This action does nothing. It is used for testing and debugging.",
        shortExample = "dummy:"
)
public class DummyAction extends QBaseAction {

    @Override
    public void play(Quester quester) {
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
