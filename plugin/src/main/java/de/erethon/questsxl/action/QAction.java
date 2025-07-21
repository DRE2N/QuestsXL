package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.SupportsActions;
import de.erethon.questsxl.common.SupportsConditions;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;

public interface QAction extends QComponent, SupportsConditions, SupportsActions {

    void play(Quester quester);
    void onFinish(Quester quester);
    boolean conditions(Quester player);

    void delayedEnd(int seconds);

    void cancel();

    Material getIcon();

    String getID();


}
