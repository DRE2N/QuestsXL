package de.erethon.questsxl.common;

import java.util.function.Consumer;

public interface QComponent extends ContextAware {

    default void load(QConfig cfg) {
        //
    }
}
