package de.erethon.questsxl.common;

/**
 * Represents an object that is aware of its context. This can be any QComponent, such as a quest, objective, or action.
 */
public interface ContextAware {

    /**
     * @return the parent of this object. This can be any QComponent, such as a quest, objective, or action
     */
    default QComponent getParent() {
        return findTopParent();
    }

    default void setParent(QComponent parent) {

    }

    /**
     * @return the top parent of this object. In most cases, this will be the quest or event that contains this object.
     */
    default QComponent findTopParent() {
        QComponent parent = getParent();
        if (parent == null) {
            return null;
        }
        while (parent.getParent() != null) {
            parent = parent.getParent();
        }
        return parent;
    }
}
