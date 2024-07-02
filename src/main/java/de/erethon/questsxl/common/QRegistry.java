package de.erethon.questsxl.common;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class QRegistry<T extends QLoadable> {

    private final Map<String, Supplier<T>> entries = new HashMap<>();

    public void register(String id, Supplier<T> entry) {
        entries.put(id, entry);
    }

    public T get(String id) {
        Supplier<T> supplier = entries.get(id);
        if (supplier != null) {
            return supplier.get();
        }
        return null;
    }

    public void unregister(String id) {
        entries.remove(id);
    }

    public boolean isValid(String id) {
        return entries.containsKey(id);
    }

    public int size() {
        return entries.size();
    }

}
