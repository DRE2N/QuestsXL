package de.erethon.questsxl.livingworld.explorables;

import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.livingworld.Explorable;
import org.bukkit.Location;
import org.bukkit.block.Chest;

public class LootChest implements Explorable {

    private Chest chestBlock;
    private Location chestLocation;

    @Override
    public String id() {
        return "";
    }

    @Override
    public QTranslatable displayName() {
        return null;
    }

    @Override
    public Location location() {
        return chestBlock.getLocation();
    }

    public static LootChest fromQLineConfig(QLineConfig config) {
        return null;
    }

    public QLineConfig toQLineConfig() {
        QLineConfig cfg = new QLineConfig();
        return cfg;
    }
}
