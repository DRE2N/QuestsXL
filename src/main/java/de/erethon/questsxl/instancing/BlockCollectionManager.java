package de.erethon.questsxl.instancing;

import de.erethon.commons.chat.MessageUtil;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BlockCollectionManager {

    List<InstancedBlockCollection> collections = new ArrayList<>();

    public BlockCollectionManager(File folder) {
        load(folder);
    }

    public InstancedBlockCollection getByID(String id) {
        for (InstancedBlockCollection c : collections) {
            if (c.getId().equalsIgnoreCase(id)) {
                return c;
            }
        }
        return null;
    }

    public List<InstancedBlockCollection> getCollections() {
        return collections;
    }

    public void load(File folder) {
        for (File file : folder.listFiles()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = file.getName().replaceAll(".yml","");
            InstancedBlockCollection collection = new InstancedBlockCollection(id);
            collection.load(config);
            collections.add(collection);
        }
        MessageUtil.log("Loaded " + collections.size() + " block collections.");
    }



}
