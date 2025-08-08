package de.erethon.questsxl.region;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QRegionManager {

    File regionData;
    private final List<QRegion> regions = new ArrayList<>();
    private final Set<QRegion> cache = new HashSet<>();

    public QRegionManager(File regionDataFile) {
        regionData = regionDataFile;
        new CacheCleanTask().runTaskTimer(QuestsXL.get(), 6000, 18000); // 15 min
        load();
    }

    @Nullable
    public QRegion getByLocation(Location location) {
        for (QRegion region : cache) {
            if (region.isInRegion(location)) {
                region.lastAccessed = System.currentTimeMillis();
                return region;
            }
        }
        for (QRegion region : regions) {
            if (region.isInRegion(location)) {
                cache.add(region);
                region.lastAccessed = System.currentTimeMillis();
                return region;
            }
        }
        return null;
    }
    @Nullable
    public QRegion getByID(String id) {
        for (QRegion region : regions) {
            if (region.getId().equalsIgnoreCase(id)) {
                return region;
            }
        }
        return null;
    }

    public List<QRegion> getRegions() {
        return regions;
    }

    public void removeFromCache(Set<QRegion> regions) {
        cache.removeAll(regions);
    }

    public Set<QRegion> getCache() {
        return cache;
    }

    public void load() {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(regionData);
        for (String string : configuration.getKeys(false)) {
            QRegion region = new QRegion(string);
            region.load(configuration.getConfigurationSection(string));
            regions.add(region);
        }
        QuestsXL.log("Loaded " + regions.size() + " regions.");
    }

    public void save() {
        YamlConfiguration regionDataFile = new YamlConfiguration();
        for (QRegion region : regions) {
            ConfigurationSection section = region.save();
            regionDataFile.set(region.getId(), section);
        }
        try {
            regionDataFile.save(regionData);
        } catch (IOException e) {
            QuestsXL.log("There was an error saving the regions.yml file!");
            e.printStackTrace();
        }
        QuestsXL.log("Saved " + regions.size() + " regions.");
    }
}

class CacheCleanTask extends BukkitRunnable {
    @Override
    public void run() {
        Set<QRegion> toRemove = new HashSet<>();
        long time = System.currentTimeMillis();
        QRegionManager manager = QuestsXL.get().getRegionManager();
        for (QRegion region : manager.getCache()) {
            if (region.lastAccessed + 300000 < time) {
                toRemove.add(region);
            }
        }
        manager.removeFromCache(toRemove);
    }
}