package de.erethon.questsxl.common;

import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.quest.QQuest;

import java.util.Set;

public interface QConfig {

    String getName();
    int getInt(String path);
    int getInt(String path, int def);
    double getDouble(String path);
    double getDouble(String path, double def);
    long getLong(String path);
    long getLong(String path, long def);
    String getString(String path);
    String getString(String path, String def);
    boolean getBoolean(String path);
    boolean getBoolean(String path, boolean def);
    String[] getStringArray(String path);
    String[] getStringArray(String path, String[] def);
    QLocation getQLocation(String path);
    QLocation getQLocation(String path, QLocation def);
    boolean contains(String path);
    Set<QAction> getActions(QComponent component, String path);
    Set<QCondition> getConditions(QComponent component, String path);
    Set<QObjective> getObjectives(QComponent component, String path);

    QEvent getQEvent(String event);
    QQuest getQuest(String quest);
}
