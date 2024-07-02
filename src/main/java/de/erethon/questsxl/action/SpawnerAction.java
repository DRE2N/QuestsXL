package de.erethon.questsxl.action;

import de.erethon.aether.Aether;
import de.erethon.aether.spawning.SpawnerManager;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class SpawnerAction extends QBaseAction {

    private final Aether aether = plugin.getAether();
    private final SpawnerManager spawnerManager = aether.getSpawnerManager();
    private String spawnerID;

    @Override
    public void play(QPlayer player) {
        super.play(player);
        spawnerManager.triggerSpawner(spawnerID);
    }

    @Override
    public void play(QEvent event) {
        super.play(event);
        spawnerManager.triggerSpawner(spawnerID);
    }

    @Override
    public void load(QLineConfig cfg) {
        super.load(cfg);
        spawnerID = cfg.getString("spawnerID");
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        spawnerID = section.getString("spawnerID");
    }
}
