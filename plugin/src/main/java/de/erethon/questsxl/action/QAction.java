package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;

public interface QAction extends QComponent {

    void play(Quester quester);
    void play(QPlayer player);
    void play(QEvent event);
    void onFinish(QPlayer player);
    void onFinish(QEvent event);
    void onFinish(Quester quester);

    boolean conditions(Quester player);
    boolean conditions(QPlayer player);
    boolean conditions(QEvent event);

    void delayedEnd(int seconds);

    void cancel();

    Material getIcon();

    String getID();


}
