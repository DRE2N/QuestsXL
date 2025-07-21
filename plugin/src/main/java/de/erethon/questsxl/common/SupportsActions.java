package de.erethon.questsxl.common;

import de.erethon.questsxl.action.QAction;

public interface SupportsActions {
    void addCompleteAction(QAction action);
    void addFailAction(QAction action);
    void addSuccessAction(QAction action);
    void addProgressAction(QAction action);
    void addRunAfterAction(QAction action);
    void addConditionFailAction(QAction action);
}