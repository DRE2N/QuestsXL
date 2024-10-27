package de.erethon.questsxl.action;

import de.erethon.aether.Aether;
import de.erethon.aether.spawning.SpawnerManager;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "spawner",
        description = "Currently only triggers a spawner. Spawners can be set-up in Aether. \nThis will be expanded in the future.",
        shortExample = "spawner: spawner=example_spawner",
        longExample = {
                "spawner:",
                "  spawner: example_spawner"
        }
)
public class SpawnerAction extends QBaseAction {

    private final Aether aether = plugin.getAether();
    private final SpawnerManager spawnerManager = aether.getSpawnerManager();

    @QParamDoc(name = "spawner", description = "The ID of the spawner to trigger", required = true)
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
    public void load(QConfig cfg) {
        super.load(cfg);
        spawnerID = cfg.getString("spawner");
    }
}
