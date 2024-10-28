package de.erethon.questsxl.common;

import net.kyori.adventure.text.Component;

import java.util.List;

public interface Displayable {
    Component getTitle();
    List<Component> getDescription();
    int getPriority();
}
