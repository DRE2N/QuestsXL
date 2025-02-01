package de.erethon.questsxl.condition;

import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

public interface QCondition extends QComponent {

    boolean check(QPlayer player);
    boolean check(QEvent event);
    boolean fail(QPlayer player);
    boolean fail(QEvent event);
    boolean success(QPlayer player);
    boolean success(QEvent event);

    String getDisplayText();
}
