package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.SupportsActions;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

public interface QCondition extends QComponent, SupportsActions {

    boolean check(Quester quester);
    boolean fail(Quester quester);
    boolean success(Quester quester);

    String getDisplayText();
}
