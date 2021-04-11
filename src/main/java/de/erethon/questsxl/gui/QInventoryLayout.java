package de.erethon.questsxl.gui;

import de.erethon.vignette.api.InventoryGUI;
import de.erethon.vignette.api.component.Component;
import de.erethon.vignette.api.layout.Layout;
import de.erethon.vignette.api.layout.SingleInventoryLayout;

public class QInventoryLayout extends SingleInventoryLayout {

    private InventoryGUI gui;

    protected Component<?, InventoryGUI>[] components;
    protected int slot;

    public QInventoryLayout(InventoryGUI gui, int size) {
        super(gui, size);
    }

    protected QInventoryLayout(InventoryGUI gui, QInventoryLayout layout) {
        super(gui, layout);
    }

    @Override
    public boolean set(int slot, Component<?, InventoryGUI> component) {
        return super.set(slot, component);
    }

    @Override
    public int nextSlot() {
        slot++;
        if (slot >= getSize()) {
            slot = -1;
        }
        return slot;
    }

    @Override
    public Layout<InventoryGUI> copy(InventoryGUI inventoryGUI) {
        return new QInventoryLayout(gui, this);
    }
}