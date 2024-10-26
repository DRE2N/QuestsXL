package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class TeleportPlayerAction extends QBaseAction{

    QLocation target;

    public void play(QPlayer player) {
        if (!conditions(player)) return;
        player.getPlayer().teleport(target.get(player.getPlayer().getLocation()));
        onFinish(player);
    }

    public Material getIcon() {
        return Material.ENDER_PEARL;
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        target = cfg.getQLocation("target");
    }
}
