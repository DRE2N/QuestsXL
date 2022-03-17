package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
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
    public void play(Player player) {
        if (!conditions(player)) return;
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
        onFinish(player);
    }

    @Override
    public Material getIcon() {
        return Material.COMMAND_BLOCK;
    }

    @Override
    public void load(String[] msg) {
        command = msg[0];
        op = Boolean.parseBoolean(msg[1]);
        console = Boolean.parseBoolean(msg[2]);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        command = section.getString("command");
        op = section.getBoolean("op");
        console = section.getBoolean("console");
    }
}
