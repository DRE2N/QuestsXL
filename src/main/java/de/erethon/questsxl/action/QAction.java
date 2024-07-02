package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QLoadable;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;

public interface QAction extends QLoadable {

    void play(QPlayer player);
    void play(QEvent event);
    void onFinish(QPlayer player);
    void onFinish(QEvent event);

    boolean conditions(QPlayer player);

    void delayedEnd(int seconds);

    void cancel();

    Material getIcon();

    String getID();


}
