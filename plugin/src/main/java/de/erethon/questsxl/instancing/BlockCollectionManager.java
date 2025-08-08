package de.erethon.questsxl.instancing;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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
        QuestsXL.log("Loaded " + collections.size() + " block collections.");
    }

    public void save() {
        for (InstancedBlockCollection ibc : collections) {
            YamlConfiguration configuration = ibc.save();
            try {
                configuration.save(new File(QuestsXL.IBCS + "/" + ibc.getId() + ".yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
