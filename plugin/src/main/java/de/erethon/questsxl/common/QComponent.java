package de.erethon.questsxl.common;

/**
 * A QComponent is a part of the QuestsXL plugin that can be loaded and unloaded from a QConfig.
 * QComponents can be nested and usually have a parent component.
 */
public interface QComponent extends ContextAware {

    default void load(QConfig cfg) {
        //
    }
}
