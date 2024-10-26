package de.erethon.questsxl.respawn;

import de.erethon.questsxl.QuestsXL;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RespawnPointManager implements Listener {

    QuestsXL plugin = QuestsXL.getInstance();

    List<RespawnPoint> points = new ArrayList<>();

    public RespawnPointManager(File file) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void load(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            RespawnPoint point = new RespawnPoint(key);
            point.load(config.getConfigurationSection(key));
            points.add(point);
        }
    }

    public void save() {
        YamlConfiguration configuration = new YamlConfiguration();
        for (RespawnPoint point : points) {
            configuration.set(String.valueOf(point.getId()), point.save());
        }
        try {
            configuration.save(QuestsXL.RESPAWNS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
