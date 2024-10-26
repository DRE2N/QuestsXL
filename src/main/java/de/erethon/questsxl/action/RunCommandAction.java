package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class RunCommandAction extends QBaseAction {

    private transient final QuestsXL plugin = QuestsXL.getInstance();
    private String command;
    private boolean op = false;
    private boolean console = false;

    @Override
    public void play(QPlayer qPlayer) {
        if (!conditions(qPlayer)) return;
        Player player = qPlayer.getPlayer();
        command = command.replace("%player%", player.getName());
        if (console) {
            plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        } else if (op) {
            player.setOp(true);
            player.performCommand(command);
            player.setOp(false);
        } else {
            player.performCommand(command);
        }
        onFinish(qPlayer);
    }

    @Override
    public Material getIcon() {
        return Material.COMMAND_BLOCK;
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        command = cfg.getString("command");
        op = cfg.getBoolean("op", false);
        console = cfg.getBoolean("console", false);
    }
}
