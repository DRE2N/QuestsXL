package de.erethon.questsxl.common;

/**
 * Represents an object that is aware of its context. This can be any QComponent, such as a quest, objective, or action.
 */
public interface ContextAware {

    /**
     * @return the parent of this object. This can be any QComponent, such as a quest, objective, or action
     */
    QComponent getParent();

    void setParent(QComponent parent);

    /**
     * @return the top parent of this object. In most cases, this will be the quest or event that contains this object.
     */
    default QComponent findTopParent() {
        QComponent current = (QComponent) this;
        if (current.getParent() == null) {
            return current;
        }
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }
}
