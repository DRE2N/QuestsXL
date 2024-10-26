package de.erethon.questsxl.common;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class QRegistry<T extends QLoadable> {

    private final Map<String, Supplier<T>> entries = new HashMap<>();
    private final Map<Class<? extends T>, String> reverseLookup = new HashMap<>();

    public void register(String id, Supplier<T> entry) {
        entries.put(id, entry);
        reverseLookup.put((Class<? extends T>) entry.get().getClass(), id);
    }

    public T get(String id) {
        Supplier<T> supplier = entries.get(id);
        if (supplier != null) {
            return supplier.get();
        }
        return null;
    }

    public void unregister(String id) {
        T entry = get(id);
        if (entry != null) {
            reverseLookup.remove(entry.getClass());
        }
        entries.remove(id);
    }

    public boolean isValid(String id) {
        return entries.containsKey(id);
    }

    public int size() {
        return entries.size();
    }

    public String getId(Class<? extends T> clazz) {
        return reverseLookup.get(clazz);
    }
}